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

package pub.rag.core.strategy;

import lombok.NonNull;
import pub.rag.core.*;
import pub.rag.core.entity.PromptContext;
import pub.rag.core.response.AdaptiveDecisionResponse;
import pub.rag.core.response.RetrievalQualityResponse;
import pub.rag.core.utils.SimilarityUtil;

import java.util.HashMap;
import java.util.List;

public class AdaptiveRetrievalStrategyScheduler<T extends RetrievalQualityResponse>
        extends AbstractRetrievalStrategyScheduler<T> {

    private final ChatModelDelegator adaptiveRouter;
    private final ChatModelDelegator nonRetrievalProxy;

    private final AbstractRetrievalStrategyScheduler<T> singleRetrievalStrategy;
    private final AbstractRetrievalStrategyScheduler<T> multiRoundRetrievalStrategy;

    public AdaptiveRetrievalStrategyScheduler(final int qualityThreshold,
                                              final int maxRetryTimes,
                                              @NonNull String template,
                                              @NonNull ChatModelDelegator invoker,
                                              @NonNull Preprocessor preprocessor,
                                              @NonNull Retriever retriever,
                                              @NonNull Aligner aligner,
                                              @NonNull Reranker reranker,
                                              @NonNull ChatModelDelegator adaptiveRouter,
                                              @NonNull ChatModelDelegator nonRetrievalProxy,
                                              @NonNull AbstractRetrievalStrategyScheduler<T> singleRetrievalStrategy,
                                              @NonNull AbstractRetrievalStrategyScheduler<T> multiRoundRetrievalStrategy) {
        super(template, invoker, preprocessor, retriever, aligner, reranker);
        this.adaptiveRouter                 = adaptiveRouter;
        this.nonRetrievalProxy              = nonRetrievalProxy;
        this.singleRetrievalStrategy        = singleRetrievalStrategy;
        this.multiRoundRetrievalStrategy    = multiRoundRetrievalStrategy;
    }

    @Override
    public String answer(String rawQuery) {
        var outNERs = new HashMap<String, String>();
        var rewritedQueries = super.preprocessor.process(rawQuery, outNERs);
        var rewritedQuery = SimilarityUtil.getBestRewrittenQuery(rawQuery, rewritedQueries);
        var promptContext = new PromptContext(super.template, rawQuery, outNERs, List.of(rewritedQuery), null, null, null, null);
        var adaptiveDecisionResponse = this.adaptiveRouter.invoke(3, true, promptContext, AdaptiveDecisionResponse.class);
        var type = adaptiveDecisionResponse.getRetrievalType();

        if (type == null) {
            throw new NullPointerException("AdaptiveDecisionResponse retrievalType is null");
        }

        return switch (type) {
            case "NO_RETRIEVAL" -> {
                var resp = nonRetrievalProxy.invoke(3, true, promptContext, RetrievalQualityResponse.class);
                yield resp.getAnswer();
            }
            case "SINGLE_RETRIEVAL" -> singleRetrievalStrategy.answer(rawQuery);
            case "MULTI_HOP_RETRIEVAL" -> multiRoundRetrievalStrategy.answer(rawQuery);
            default -> throw new IllegalArgumentException("Unsupported retrieval type: " + type);
        };
    }
}