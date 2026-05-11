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
import com.github.wordsless.galaxy.core.response.AbstractBasicResponse;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.wordsless.galaxy.core.exception.ChatModelInvokerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ChatModelDelegator<T> {

    private static final Logger logger = LoggerFactory.getLogger(ChatModelDelegator.class);

    /**
     * Maximum prompt length, adjustable based on actual LLM configuration.
     */
    private static final int MAX_PROMPT_LENGTH = 8192;

    protected final ChatModel chatModel;

    protected final ObjectMapper mapper;

    @Autowired
    public ChatModelDelegator(ChatModel chatModel, ObjectMapper mapper) {
        this.chatModel = chatModel;
        this.mapper = mapper;
    }

    /**
     * Execute LLM call with JSON Schema validation and auto-retry support.
     *
     * @param maxRetryCount maximum retry count (only for JSON format/deserialization failures)
     * @return deserialized target object
     */
    public T delegate(final int maxRetryCount,
                      final ChatModelRequest request,
                      final TypeReference<T> typeReference) {
        // Input parameter validation
        if (maxRetryCount < 1) {
            throw new ChatModelInvokerException("Max retry count must be greater than 0");
        }
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        Objects.requireNonNull(mapper, "ObjectMapper cannot be null");

        // Loop retry (non-recursive to avoid stack overflow)
        for (int retryCount = 0; retryCount < maxRetryCount; retryCount++) {
            try {
                var prompt = toString(request);
                // 2. Call LLM
                logger.info("Start invoking LLM, current retry count: {}", retryCount);
                String response = chatModel.chat(prompt);

                // Validate non-empty response
                if (response == null || response.isBlank()) {
                    throw new ChatModelInvokerException("LLM returned empty response");
                }
                // Log raw response for troubleshooting
                logger.debug("LLM raw response: {}", response);

                // 4. Deserialize JSON to target object
                T result = mapper.readValue(response, mapper.constructType(typeReference.getType()));
                logger.info("LLM invocation succeeded, deserialization completed, retry count: {}", retryCount);
                return result;
            } catch (JsonProcessingException e) {
                // JSON deserialization exception only
                logger.error("JSON deserialization failed, current retry count: {}", retryCount, e);
                if (retryCount == maxRetryCount - 1) {
                    throw new ChatModelInvokerException("JSON deserialization failed, reached max retry count: " + maxRetryCount, e);
                }
            } catch (ChatModelInvokerException e) {
                // Business exceptions (empty prompt, length exceeded, empty response, etc.)
                logger.error("LLM call business exception, retry count: {}, message: {}", retryCount, e.getMessage(), e);
                if (retryCount == maxRetryCount - 1) {
                    throw e;
                }
            } catch (Exception e) {
                // Other unknown exceptions
                logger.error("LLM call execution exception, current retry count: {}", retryCount, e);
                if (retryCount == maxRetryCount - 1) {
                    throw new ChatModelInvokerException("LLM call failed, reached max retry count: " + maxRetryCount, e);
                }
            }
        }

        // All retries exhausted without success
        throw new ChatModelInvokerException("LLM call failed, reached max retry count: " + maxRetryCount);
    }

    public T delegate(final ChatModelRequest<T> request,
                      final TypeReference<T> typeReference) {
        return this.delegate(3, request, typeReference);
    }
}