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

package com.github.wordsless.galaxy.core.solution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.*;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.orchestrator.Orchestrator;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultRAGSolution {

    private final Preprocessor preprocessor;

    private final Orchestrator orchestrator;

    private final Aligner aligner;

    private final Reranker reranker;

    private final List<Evaluator> evaluators;

    private final Retrier retrier;

    private final ChatModelRequest generateAnswerRequest;

    private final ChatModelDelegator<Map<String, ?>> chatModelDelegator;

    @Value("max-retry-count")
    private final int maxRetryCount;

    public DefaultRAGSolution(final Preprocessor preprocessor,
                              @NonNull final Orchestrator orchestrator,
                              final Aligner aligner,
                              final Reranker reranker,
                              final List<Evaluator> evaluators,
                              final Retrier retrier,
                              @NonNull final ChatModelRequest generateAnswerRequest,
                              @NonNull final ChatModelDelegator<Map<String, ?>> chatModelDelegator,
                              final int maxRetryCount) {
        this.preprocessor       = preprocessor;
        this.orchestrator       = orchestrator;
        this.aligner            = aligner;
        this.reranker           = reranker;
        this.evaluators         = evaluators;
        this.retrier            = retrier;
        this.generateAnswerRequest = generateAnswerRequest;
        this.chatModelDelegator = chatModelDelegator;
        this.maxRetryCount      = maxRetryCount;
    }

    public String answer(final String rawQuery) {
        boolean retry = true;
        int c = 0;
        while(retry && c < maxRetryCount) {
            var context = new Context();
            var query = new Query();
            query.setText(rawQuery);
            context.setQuery(query);
            if(this.preprocessor != null)
                context = this.preprocessor.next(context);
            var docs = this.orchestrator.retrieve(context);
            if(this.aligner != null && this.reranker != null) {
                for(var pair : docs) {
                    var list = pair.getValue();
                    var aligned  = this.aligner.align(list, 10000, 10);
                    var reranked = this.reranker.rerank(aligned, context);
                    pair.setValue(reranked);
                }
            } else if(this.aligner == null && this.reranker != null) {
                for(var pair : docs) {
                    var list = pair.getValue();
                    var reranked = this.reranker.rerank(list, context);
                    pair.setValue(reranked);
                }
            } else if(this.aligner != null/*&& this.reranker == null*/) { // when code has run here, means reranker must be true.
                for(var pair : docs) {
                    var list = pair.getValue();
                    var aligned  = this.aligner.align(list, 10000, 10);
                    pair.setValue(aligned);
                }
            }
            context.setReferences(docs);
            var request = generateAnswerRequest.withContext(context);
            var results = this.chatModelDelegator.delegate(request, new TypeReference<Map<String, ?>>() {});
            if(this.evaluators != null && !this.evaluators.isEmpty() && this.retrier != null) {
                for(var evaluator : this.evaluators) {
                    evaluator.evaluate(results);
                }
                retry = retrier.retrie(results);
            } else {
                retry = false;
            }
            c++;
        }
        return retrier.best();
    }
}
