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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.algorithm.air.SystemAnalysis;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.google.common.io.Resources;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Configuration
@PropertySource("classpath:deepseek.properties")
public class ChatModelRequestConfig {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    // Request prompt to chat model during MCTS simulation
    @Bean("simulationRequest")
    public ChatModelRequest simulationRequest(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/simulation.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Request prompt to chat model during MCTS expansion of a SAY (SystemAnalysis) node.
    @Bean("expansionRequest4SAY")
    public ChatModelRequest expansionRequest4SAY(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/expansion/SAY.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Request prompt to chat model during MCTS expansion of a SA (Summary Answer) node.
    @Bean("expansionRequest4SA")
    public ChatModelRequest expansionRequest4SA(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/expansion/SA.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Request prompt to chat model during MCTS expansion of a DA (Directly Answer) node.
    @Bean("expansionRequest4DA")
    public ChatModelRequest expansionRequest4DA(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/expansion/DA.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Request prompt to chat model during MCTS expansion of a DA (Directly Answer) node.
    @Bean("expansionRequest4RA")
    public ChatModelRequest expansionRequest4RA(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/expansion/RA.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Request prompt to chat model during MCTS expansion of a DA (Directly Answer) node.
    @Bean("expansionRequest4QT")
    public ChatModelRequest expansionRequest4QT(final ObjectMapper objectMapper) {
        try {
            String template = Resources.toString(Resources.getResource("prompts/mcts/expansion/QT.json"), StandardCharsets.UTF_8);
            return objectMapper.readValue(template, new TypeReference<ChatModelRequest>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ChatModel cloudChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .build();
    }

    @Bean("simulationChatModelDelegator")
    public ChatModelDelegator<List<Double>>
    simulationChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<Double>>(chatModel,  objectMapper);
    }

    @Bean("directAnswerResponseChatModelDelegator")
    public ChatModelDelegator<List<String>>
    directAnswerResponseChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<String>>(chatModel,  objectMapper);
    }

    @Bean("retrievalAnswerResponseChatModelDelegator")
    public ChatModelDelegator<List<String>>
    retrievalAnswerResponseChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<String>>(chatModel,  objectMapper);
    }

    @Bean("systemAnalysisChatModelDelegator")
    public ChatModelDelegator<SystemAnalysis>
    systemAnalysisChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<SystemAnalysis>(chatModel,  objectMapper);
    }

    @Bean("queryTransformChatModelDelegator")
    public ChatModelDelegator<List<String>>
    queryTransformChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<String>>(chatModel,  objectMapper);
    }

    @Bean("summaryAnswerChatModelDelegator")
    public ChatModelDelegator<List<String>>
    summaryAnswerChatModelDelegator(ChatModel chatModel, ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<String>>(chatModel,  objectMapper);
    }
}
