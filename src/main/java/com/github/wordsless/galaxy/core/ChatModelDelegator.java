/*
 * MIT License
 *
 * Copyright (c) ${YEAR} Qiang Li (李强)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.wordsless.galaxy.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import com.github.wordsless.galaxy.core.exception.ChatModelInvokerException;
import com.github.wordsless.galaxy.core.entity.Context;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class ChatModelDelegator<T> {

    protected final ChatModel chatModel;

    protected final ObjectMapper objectMapper;

    protected final int maxRetryCount;

    public ChatModelDelegator(final ChatModel chatModel, final ObjectMapper objectMapper, final int maxRetryCount) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.maxRetryCount = maxRetryCount;
    }

    /**
     * Execute LLM call with JSON Schema validation and auto-retry support.
     *
     * @return deserialized target object
     */
    public T delegate(final ChatModelRequest request,
                      final TypeReference<T> typeReference,
                      final Context context) {
        if (maxRetryCount < 1) {
            throw new ChatModelInvokerException("Max retry count must be greater than 0");
        }
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");

        String msg = null;

        for (int retryCount = 0; retryCount < maxRetryCount; retryCount++) {
            try {
                var chatRequest = generateChatRequest(request, context);
                var response = chatModel.chat(chatRequest);
                if (response == null || response.aiMessage().text().isBlank()) {
                    throw new ChatModelInvokerException("LLM returned empty response");
                }
                msg = response.aiMessage().text();
                log.info("msg = {}", msg);
                return objectMapper.readValue(msg, objectMapper.constructType(typeReference.getType()));
            } catch (JsonProcessingException e) {
                log.error("JSON deserialization failed, current retry count: {}", retryCount, e);
                if (retryCount == maxRetryCount - 1) {
                    throw new ChatModelInvokerException("JSON deserialization failed, reached max retry count: " + maxRetryCount, e);
                }
            } catch (ChatModelInvokerException e) {
                log.error("LLM call business exception, retry count: {}, message: {}", retryCount, e.getMessage(), e);
                if (retryCount == maxRetryCount - 1) {
                    throw e;
                }
            } catch (Exception e) {
                log.error("LLM call execution exception, current retry count: {}", retryCount, e);
                if (retryCount == maxRetryCount - 1) {
                    throw new ChatModelInvokerException("LLM call failed, reached max retry count: " + maxRetryCount, e);
                }
            }
        }

        throw new ChatModelInvokerException("LLM call failed, reached max retry count: " + maxRetryCount);
    }

    public String buildPromptWithRequest(@NonNull final ChatModelRequest request,
                                         @NonNull final Context context) {
        var sb = new StringBuilder();
        sb.append("# Role:\n%s\n\n".formatted(request.getRole()));
        sb.append("# Task:\n%s\n\n".formatted(request.getTask()));
        sb.append("# Raw Query:\n%s\n\n".formatted(request.getQuery().toString()));
        sb.append("# Rules:\n");
        request.getRules().forEach(rule -> {
            sb.append("- %s\n".formatted(rule));
        });
        sb.append("\n");
        sb.append("# Constraints:\n");
        request.getConstraints().forEach(constraint -> {
            sb.append("- %s\n".formatted(constraint));
        });
        sb.append("\n");
        sb.append("# Output Language:\n%s\n\n".formatted(request.getOutputLanguage()));

        var conversations = context.getConversations();
        if(conversations != null && !conversations.isEmpty()) {
            sb.append("# Context:\n");
            conversations.forEach(conversation -> {
                sb.append("- %s\n".formatted(conversation.toString()));
            });
            sb.append("\n");
        }

        var outputFormat = request.getOutputFormat();
        if(outputFormat != null) {
            sb.append("# Output Format:\n");
            sb.append(outputFormat);
            sb.append("\n\n");
        }

        var outputSchema = request.getOutputSchema();
        if(outputSchema != null) {
            sb.append("# Output Schema:\n");
            sb.append(outputSchema);
            sb.append("\n\n");
        }

        var references = context.getReferences();
        if(references != null && !references.isEmpty()) {
            sb.append("# References:\n");
            references.forEach(item -> {
                var query = item.keySet().iterator().next();
                var docs = item.get(query);
                sb.append("%s\n".formatted(query.toString()));
                docs.forEach(value -> {
                    sb.append("- %s\n".formatted(value.toString()));
                });
            });
            sb.append("\n");
        }
        /*
        sb.append("# Generation Parameters:\n");

        var n = request.getN();
        if(n != null) {
            sb.append("n = %d".formatted(n));
        }

        var stream = request.getStream();
        if(stream != null) {
            sb.append("stream = %b\n".formatted(stream));
        }

        var logprobs = request.getLogprobs();
        if(logprobs != null) {
            sb.append("logprobs = %b\n".formatted(logprobs));
        }

        var echo = request.getEcho();
        if(echo != null) {
            sb.append("echo = %b\n".formatted(echo));
        }

        var seed = request.getSeed();
        if(seed != null) {
            sb.append("seed = %d\n".formatted(seed));
        }

        var bestOf = request.getBestOf();
        if(bestOf != null) {
            sb.append("best_of = %d\n".formatted(bestOf));
        }

        var logitBias = request.getLogitBias();
        if(logitBias != null) {
            for(var p : logitBias.entrySet()) {
                sb.append("%s = %d\n".formatted(p.getKey(), p.getValue()));
            }
        }
        sb.append("\n");
         */
        return sb.toString();
    }

    public ChatRequest generateChatRequest(@NonNull final ChatModelRequest request,
                                           @NonNull final Context context) {
        JsonSchema outputSchema = JsonSchema.builder()
                .name("EnumArrayResponse") // 某些模型（如 OpenAI）要求必须指定一个名字
                .rootElement(JsonRawSchema.from(request.getOutputSchema()))
                .build();
        var parameters = OpenAiChatRequestParameters.builder()
                .modelName("deepseek-v4-pro")
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxOutputTokens(request.getMaxTokens())
                .presencePenalty(request.getPresencePenalty())
                .frequencyPenalty(request.getFrequencyPenalty())
                //.topK(request.getTopK())
                .seed(request.getSeed())
                .logitBias(request.getLogitBias())
                //.responseFormat(outputSchema)
                .customParameters(Map.of("thinking", Map.of("type","disabled")))
                .build();
        return ChatRequest.builder()
                .messages(new SystemMessage(buildPromptWithRequest(request, context)))
                .parameters(parameters)
                .build();
    }
}