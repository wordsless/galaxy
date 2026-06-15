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
import java.util.Map;

/**
 * Routing Process in Refine Query RAG.
 */
public class RQRouter implements QueryRewriter {

    private ChatModelDelegator<Map<String, Boolean>> delegator;

    private ChatModelRequest request;

    public RQRouter(final ChatModelDelegator<Map<String, Boolean>> delegator, final ChatModelRequest request) {
        this.delegator = delegator;
        this.request = request;
    }

    @Override
    public List<Query> rewrite(Context context) {
        var req = this.request.withQuery(context.getQuery());
        var resp = this.delegator.delegate(req, new TypeReference<Map<String, Boolean>>() {}, context);
        if(resp.get("needRetrieval")) {
            return List.of(context.getQuery());
        } else if(resp.get("isMultiHop")) {

        }
        return List.of();
    }
}
