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

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Value("${decision.api.key:${DECISION_API_KEY}}")
    private String decisionApiKey;

    @Value("${decision.base.url}")
    private String decisionBaseUrl;

    @Value("${decision.model.name}")
    private String decisionModelName;

    @Value("${decision.model.temperature:0.7}")
    private Double decisionModelTemperature;

    @Bean("DecisionModel")
    public ChatModel getDecisionChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(decisionBaseUrl) // DeepSeek 官方地址
                .apiKey(decisionApiKey) // 从环境变量读取，避免硬编码
                .modelName(decisionModelName) // 可选：deepseek-reasoner（推理模型）
                .temperature(decisionModelTemperature) // 随机性 0~1
                .maxTokens(2048) // 最大输出长度
                .logRequests(true) // 打印请求日志（调试用）
                .logResponses(true)
                .build();
    }

    @Bean("ReFeRewriteChatMode")
    public ChatModel getReFeRewriteChatMode() {
        return OpenAiChatModel.builder()
                .baseUrl(decisionBaseUrl) // DeepSeek 官方地址
                .apiKey(decisionApiKey) // 从环境变量读取，避免硬编码
                .modelName(decisionModelName) // 可选：deepseek-reasoner（推理模型）
                .temperature(decisionModelTemperature) // 随机性 0~1
                .maxTokens(2048) // 最大输出长度
                .logRequests(true) // 打印请求日志（调试用）
                .logResponses(true)
                .build();
    }

    @Bean("RefineQueryChatMode")
    public ChatModel getRefineQueryChatMode() {
        return OpenAiChatModel.builder()
                .baseUrl(decisionBaseUrl) // DeepSeek 官方地址
                .apiKey(decisionApiKey) // 从环境变量读取，避免硬编码
                .modelName(decisionModelName) // 可选：deepseek-reasoner（推理模型）
                .temperature(decisionModelTemperature) // 随机性 0~1
                .maxTokens(2048) // 最大输出长度
                .logRequests(true) // 打印请求日志（调试用）
                .logResponses(true)
                .build();
    }
}
