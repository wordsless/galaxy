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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RQ-RAG: Learning to Refine Queries for Retrieval Augmented Generation
// https://arxiv.org/html/2404.00610v1
// https://github.com/chanchimin/RQ-RAG/
public class RefineQueryRewriter implements QueryRewriter<Map<String, List<Query>>> {

    private final ChatModelRequest actionSelectRequest;

    private final ChatModelRequest simpleRewriteRequest;

    private final ChatModelRequest decomposeRequest;

    private final ChatModelRequest disambiguateRequest;

    private final ChatModelDelegator<List<String>> routeDelegator;

    private final ChatModelDelegator<List<Query>> rewriteDelegator;

    public RefineQueryRewriter(final ChatModelDelegator<List<String>> routeDelegator,
                               final ChatModelDelegator<List<Query>> rewriteDelegator,
                               final ChatModelRequest actionSelectRequest,
                               final ChatModelRequest simpleRewriteRequest,
                               final ChatModelRequest decomposeRequest,
                               final ChatModelRequest disambiguateRequest) {
        this.routeDelegator = routeDelegator;
        this.actionSelectRequest = actionSelectRequest;
        this.simpleRewriteRequest = simpleRewriteRequest;
        this.decomposeRequest = decomposeRequest;
        this.disambiguateRequest = disambiguateRequest;
        this.rewriteDelegator = rewriteDelegator;
    }

    @Override
    public Map<String, List<Query>> rewrite(Context context) {
        var rewritten = new HashMap<String, List<Query>>();
        var request = actionSelectRequest.withQuery(context.getQuery());
        var sub = this.routeDelegator.delegate(request, new TypeReference<List<String>>() {}, context);
        for(var item : sub) {
            if(item.equals("DR")) {
                rewritten.put("DR", List.of(context.getQuery()));
            } else if(item.equals("SAR")) {
                request = simpleRewriteRequest.withQuery(context.getQuery());
                var results = rewriteDelegator.delegate(request, new TypeReference<List<Query>>() {}, context);
                rewritten.put("SAR", results);
            } else if(item.equals("PR")) {
                request = disambiguateRequest.withQuery(context.getQuery());
                var results = rewriteDelegator.delegate(request, new TypeReference<List<Query>>() {}, context);
                rewritten.put("PR", results);
            } else {
                request = decomposeRequest.withQuery(context.getQuery());
                var results = rewriteDelegator.delegate(request, new TypeReference<List<Query>>() {}, context);
                rewritten.put("VHR", results);
            }
        }
        return rewritten;
    }
}
