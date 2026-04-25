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

package pub.rag.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.rag.core.entity.PromptContext;
import pub.rag.core.exception.ChatModelInvokerException;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class ChatModelDelegator {

    private static final Logger logger = LoggerFactory.getLogger(ChatModelDelegator.class);

    /**
     * Maximum prompt length, adjustable based on actual LLM configuration.
     */
    private static final int MAX_PROMPT_LENGTH = 8192;

    protected final ChatModel chatModel;
    protected final Schema schema;
    protected final ObjectMapper mapper;
    protected final PromptProvider promptProvider;

    // ==================== 仅保留Builder模式 ====================
    /**
     * 流式创建器（Builder模式）：支持链式配置
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== 内部Builder类 ====================
    public static class Builder {
        private ChatModel chatModel;
        private Schema schema;
        private ObjectMapper mapper;
        private PromptProvider promptProvider;

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        public Builder mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder promptProvider(PromptProvider promptProvider) {
            this.promptProvider = promptProvider;
            return this;
        }

        public ChatModelDelegator build() {
            // 核心校验：必须配置ChatModel
            if (chatModel == null) {
                throw new ChatModelInvokerException("ChatModel must be configured");
            }
            // 基础非空校验
            if (mapper == null) {
                throw new ChatModelInvokerException("ObjectMapper must be configured");
            }
            if (promptProvider == null) {
                throw new ChatModelInvokerException("PromptProvider must be configured");
            }
            return new ChatModelDelegator(chatModel, schema, mapper, promptProvider);
        }
    }

    /**
     * Validate prompt content and length.
     * @param prompt prompt text to validate
     */
    private void validatePromptLength(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new ChatModelInvokerException("Prompt cannot be empty");
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new ChatModelInvokerException(
                    "Prompt length exceeds limit, current length: " + prompt.length() + ", max limit: " + MAX_PROMPT_LENGTH
            );
        }
    }

    /**
     * Execute LLM call with JSON Schema validation and auto-retry support.
     *
     * @param maxRetryCount maximum retry count (only for JSON format/deserialization failures)
     * @param validatable whether to enable JSON Schema validation
     * @param context prompt building context
     * @return deserialized target object
     */
    public <T> T invoke(final int maxRetryCount, final boolean validatable, final PromptContext context, final Class<T> resultType) {
        // Input parameter validation
        if (maxRetryCount < 1) {
            throw new ChatModelInvokerException("Max retry count must be greater than 0");
        }
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        Objects.requireNonNull(mapper, "ObjectMapper cannot be null");
        Objects.requireNonNull(promptProvider, "PromptProvider cannot be null");
        if (validatable && schema == null) {
            throw new ChatModelInvokerException("Schema cannot be null when JSON Schema validation is enabled");
        }

        // Current context (carries error messages for retry, fixed original logic bug)
        PromptContext currentContext = context;

        // Loop retry (non-recursive to avoid stack overflow)
        for (int retryCount = 0; retryCount < maxRetryCount; retryCount++) {
            try {
                // 1. Build prompt and validate length
                String prompt = promptProvider.buildPrompt(currentContext);
                validatePromptLength(prompt);

                // 2. Call LLM
                logger.info("Start invoking LLM, current retry count: {}", retryCount);
                String response = chatModel.chat(prompt);

                // Validate non-empty response
                if (response == null || response.isBlank()) {
                    throw new ChatModelInvokerException("LLM returned empty response");
                }
                // Log raw response for troubleshooting
                logger.debug("LLM raw response: {}", response);

                // 3. JSON Schema validation
                if (validatable) {
                    List<String> errors = schema.validate(response, InputFormat.JSON)
                            .stream()
                            .map(error -> error.getMessage())
                            .toList();

                    if (!errors.isEmpty()) {
                        logger.warn("JSON Schema validation failed, errors: {}, retry count: {}", errors, retryCount);
                        // Use current context to carry errors for retry, not original context
                        currentContext = currentContext.withErrors(errors);
                        continue;
                    }
                }

                // 4. Deserialize JSON to target object
                T result = mapper.readValue(response, resultType);
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
}