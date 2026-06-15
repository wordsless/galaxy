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

package com.github.wordsless.galaxy.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ChatModelRequestConfig {

    @Value("${decision.model.name}")
    private String decisionModelName;

    @Bean("NamedEntityRecognizeRequest")
    public ChatModelRequest getNERsChatModelRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/NER.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("ReFeQueryWriteChatModelRequest")
    public ChatModelRequest
    getReFeQueryWriteChatModelRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/ReFe/RaFeQueryRewrite.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("AdaptiveStrategySelectionChatModelRequest")
    public ChatModelRequest
    getAdaptiveStrategySelectionChatModelRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/DMQR/adaptive_strategy_selection.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("GQRRequest")
    public ChatModelRequest
    getGeneralQueryRewritingRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/DMQR/GQR.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("KWRRequest")
    public ChatModelRequest
    getKeywordRewritingRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/DMQR/KWR.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("PARRequest")
    public ChatModelRequest
    getPseudoAnswerRewritingRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/DMQR/PAR.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("CCERequest")
    public ChatModelRequest
    getCoreContentExtractionRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/DMQR/CCE.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("RefineQueryRewriteActionSelect")
    public ChatModelRequest
    getRefineQueryRewriteActionSelectRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/RQ/refine_query_rewrite_action_select.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel("Qwen/Qwen3.5-9B");
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("SimpleQueryRewrite")
    public ChatModelRequest
    getSimpleQueryRewriteRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/RQ/simple_query_rewriting.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("VerticalRewrite")
    public ChatModelRequest
    getVerticalRewriteRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/RQ/vertical_query_rewriting.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("ParallelRewrite")
    public ChatModelRequest
    getParallelRewriteRequest(final ObjectMapper objectMapper) {
        try(var input = Thread.currentThread().getContextClassLoader().getResourceAsStream("prompts/RQ/parallel_query_rewriting.json")) {
            var request = objectMapper.readValue(input, ChatModelRequest.class);
            request.setModel(decisionModelName);
            return request;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
