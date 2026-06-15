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
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.Query;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatModelDelegatorConfig {

    @Value("${chat_model.request.retry:3}")
    private int chatModelRequestRetryCount;

    @Bean("NamedEntityRecognizeDelegator")
    public ChatModelDelegator<List<Query.Entity>>
    getNERsChatModelDelegator(@Qualifier("DecisionModel") final ChatModel model,
                              final ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<Query.Entity>>(model, objectMapper, chatModelRequestRetryCount);
    }

    @Bean("QueryRewriteChatModelDelegator")
    public ChatModelDelegator<List<Query>>
    getQueryRewriteChatModelDelegator(@Qualifier("ReFeRewriteChatMode") final ChatModel model,
                                      final ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<Query>>(model, objectMapper, chatModelRequestRetryCount);
    }

    @Bean("RouteChatModelDelegator")
    public ChatModelDelegator<List<String>>
    getRouteChatModelDelegator(@Qualifier("DecisionModel") final ChatModel model,
                               final ObjectMapper objectMapper) {
        return new ChatModelDelegator<List<String>>(model, objectMapper, chatModelRequestRetryCount);
    }
}
