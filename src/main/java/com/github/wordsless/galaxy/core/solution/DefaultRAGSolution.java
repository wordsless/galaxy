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
import com.github.wordsless.galaxy.core.ChatModelRequest;
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

    private final ChatModelRequest generateRequest;

    private final ChatModelDelegator<String> chatModelDelegator;

    @Value("max-retry-count")
    private final int maxRetryCount;

    public DefaultRAGSolution(@NonNull
                              final Preprocessor preprocessor,
                              @NonNull
                              final Orchestrator orchestrator,
                              @NonNull
                              final Aligner aligner,
                              @NonNull
                              final Reranker reranker,
                              @NonNull
                              final List<Evaluator> evaluators,
                              @NonNull
                              final Retrier retrier,
                              @NonNull
                              final ChatModelRequest generateRequest,
                              @NonNull
                              final ChatModelDelegator<String> chatModelDelegator,
                              @NonNull
                              final int maxRetryCount) {
        this.preprocessor       = preprocessor;
        this.orchestrator       = orchestrator;
        this.aligner            = aligner;
        this.reranker           = reranker;
        this.evaluators = evaluators;
        this.retrier            = retrier;
        this.generateRequest    = generateRequest;
        this.chatModelDelegator = chatModelDelegator;
        this.maxRetryCount      = maxRetryCount;
    }

    public String answer(final String rawQuery) {
        boolean retry = true;
        Map<String, ?> context = null;
        int c = 0;
        while(retry && c < maxRetryCount) {
            context = this.preprocessor.next(rawQuery);
            List<String> docs = this.orchestrator.retrieve(context);
            docs = this.aligner.align(docs);
            docs = this.reranker.rerank(docs);
            ((Map)context).put("Docs", docs);
            var request = buildRequestWithContext(context, generateRequest);
            var answer = this.chatModelDelegator.delegate(request, new TypeReference<String>() {});
            ((Map) context).put("Answer", answer);
            for(var scorer : this.evaluators) {
                scorer.evaluate(context);
            }
            retry = retrier.retrie(context);
            c++;
        }
        return (String) context.get("Answer");
    }

    public ChatModelRequest buildRequestWithContext(final Map<String, ?> context, final ChatModelRequest template) {
        var rawQuery = (String) context.get("RawQuery");
        var request = template.withRawQuery(rawQuery);
        var entities = context.entrySet();
        for(var entity : entities) {
            var key  = entity.getKey();
            if(key.equals("NERs")) {
                request.setNERs((Map<String, String>) entity.getValue());
            } else if(key.equals("RewritedQuery")) {
                request.setRawQuery(entity.getKey());
            } else if(key.equals("TOPICs")) {
                request.setTopics((List<String>) entity.getValue());
            } else if(key.equals("RewritedMultiQueries")) {
                request.setRewritedMultiQueries((List<String>) entity.getValue());
            } else if(key.equals("Docs")) {
                request.setDocuments((List<String>) entity.getValue());
            }
        }
        return request;
    }
}
