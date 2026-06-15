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

package com.github.wordsless.galaxy.core.preprocessor.rewriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.QueryRewriter;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

// Diverse Multi-Query Rewriting
// DMQR-RAG：Diverse Multi-Query Rewriting for Retrieval-Augmented Generation
// https://arxiv.org/abs/2411.13154
public class DmqrQueryRewrite implements QueryRewriter<List<Query>> {

    private final ChatModelDelegator<List<String>> adaptiveStrategySelector;

    private final ChatModelDelegator<List<Query>> queryRewriteDelegator;

    private final ChatModelRequest adaptiveStrategySelectionRequest;

    private final ChatModelRequest generalQueryRewritingRequest;

    private final ChatModelRequest keywordRewritingRequest;

    private final ChatModelRequest pseudoAnswerRewritingRequest;

    private final ChatModelRequest coreContentExtractionRequest;

    public DmqrQueryRewrite(final ChatModelDelegator<List<String>> adaptiveStrategySelector,
                            final ChatModelDelegator<List<Query>> queryRewriteDelegator,
                            final ChatModelRequest adaptiveStrategySelectionRequest,
                            final ChatModelRequest generalQueryRewritingRequest,
                            final ChatModelRequest keywordRewritingRequest,
                            final ChatModelRequest pseudoAnswerRewritingRequest,
                            final ChatModelRequest coreContentExtractionRequest) {
        this.adaptiveStrategySelector = adaptiveStrategySelector;
        this.queryRewriteDelegator = queryRewriteDelegator;
        this.adaptiveStrategySelectionRequest = adaptiveStrategySelectionRequest;
        this.generalQueryRewritingRequest = generalQueryRewritingRequest;
        this.keywordRewritingRequest = keywordRewritingRequest;
        this.pseudoAnswerRewritingRequest = pseudoAnswerRewritingRequest;
        this.coreContentExtractionRequest = coreContentExtractionRequest;
    }

    @Override
    public List<Query> rewrite(Context context) {
        ChatModelRequest request = adaptiveStrategySelectionRequest.withQuery(context.getQuery()).withContext(context);
        var strategies = adaptiveStrategySelector.delegate(request, new TypeReference<List<String>>() {}, context);
        var results = new ConcurrentSkipListSet<Query>();
        try {
            for(var strategy : strategies) {
                if(strategy.equals("GQR")) {
                    request = generalQueryRewritingRequest.withContext(context).withQuery(context.getQuery());
                } else if(strategy.equals("KWR")) {
                    request = keywordRewritingRequest.withContext(context).withQuery(context.getQuery());
                } else if(strategy.equals("PAR")) {
                    request = pseudoAnswerRewritingRequest.withContext(context).withQuery(context.getQuery());
                } else if(strategy.equals("CCE")) {
                    request = coreContentExtractionRequest.withContext(context).withQuery(context.getQuery());
                } else {
                    throw new RuntimeException("Unknown strategy: " + strategy);
                }
                var rewrittenQueries = queryRewriteDelegator.delegate(request, new TypeReference<List<Query>>() {}, context);
                results.addAll(rewrittenQueries);
            }
        } catch (Exception e) {
            throw new RuntimeException("DMQR rewrite failed", e);
        }
        return results.stream().toList();
    }
}
