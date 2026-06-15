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
import com.github.wordsless.galaxy.core.QueryRewriter;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.preprocessor.rewriter.DmqrQueryRewrite;
import com.github.wordsless.galaxy.core.preprocessor.rewriter.RefineQueryRewriter;
import com.github.wordsless.galaxy.core.preprocessor.rewriter.SingleHopQueryRewriter;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
public class QueryRewriterConfig {

    @Value("${chat_model.request.retry:3}")
    private int chatModelRequestRetryCount;

    @Bean("RefeQueryRewriter")
    public QueryRewriter<List<Query>>
    getRafeQueryRewriter(@Qualifier("QueryRewriteChatModelDelegator")
                         final ChatModelDelegator<List<Query>> delegator,
                         @Qualifier("ReFeQueryWriteChatModelRequest")
                         final ChatModelRequest request) {
        return new SingleHopQueryRewriter(delegator, request);
    }

    @Bean("DmqrQueryRewrite")
    public QueryRewriter<List<Query>>
    getDmqrQueryRewrite(@Qualifier("RouteChatModelDelegator")
                        final ChatModelDelegator<List<String>> adaptiveStrategySelector,
                        @Qualifier("QueryRewriteChatModelDelegator")
                        final ChatModelDelegator<List<Query>> generalQueryRewritingDelegator,
                        @Qualifier("AdaptiveStrategySelectionChatModelRequest")
                        final ChatModelRequest adaptiveStrategySelectionRequest,
                        @Qualifier("GQRRequest")
                        final ChatModelRequest generalQueryRewritingRequest,
                        @Qualifier("KWRRequest")
                        final ChatModelRequest keywordRewritingRequest,
                        @Qualifier("PARRequest")
                        final ChatModelRequest pseudoAnswerRewritingRequest,
                        @Qualifier("CCERequest")
                        final ChatModelRequest coreContentExtractionRequest) {

        return new DmqrQueryRewrite(adaptiveStrategySelector,
                                    generalQueryRewritingDelegator,
                                    adaptiveStrategySelectionRequest,
                                    generalQueryRewritingRequest,
                                    keywordRewritingRequest,
                                    pseudoAnswerRewritingRequest,
                                    coreContentExtractionRequest);
    }

    @Bean("RefineQueryRewriter")
    public QueryRewriter<Map<String, List<Query>>>
    getRefineQueryRewriter(@Qualifier("RouteChatModelDelegator")
                                                      final ChatModelDelegator<List<String>> actionSelectDelegator,
                                                      @Qualifier("QueryRewriteChatModelDelegator")
                                                      final ChatModelDelegator<List<Query>> generalQueryRewritingDelegator,
                                                      @Qualifier("RefineQueryRewriteActionSelect")
                                                      final ChatModelRequest actionSelectRequest,
                                                      @Qualifier("SimpleQueryRewrite")
                                                      final ChatModelRequest rewriteRouteRequest,
                                                      @Qualifier("VerticalRewrite")
                                                      final ChatModelRequest decomposeRequest,
                                                      @Qualifier("ParallelRewrite")
                                                      final ChatModelRequest disambiguateRequest) {
        return new RefineQueryRewriter(actionSelectDelegator,
                generalQueryRewritingDelegator,
                actionSelectRequest,
                rewriteRouteRequest,
                decomposeRequest,
                disambiguateRequest);
    }
}
