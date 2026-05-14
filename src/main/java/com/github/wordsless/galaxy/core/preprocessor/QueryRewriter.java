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

package com.github.wordsless.galaxy.core.preprocessor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.ChatModelRequest;

import java.util.List;
import java.util.Map;

public class QueryRewriter implements IQueryFilter {

    private final ChatModelDelegator<List<String>> chatModelDelegator;

    private final ChatModelRequest queryRewriteRequest;

    public QueryRewriter(final ChatModelDelegator<List<String>> chatModelDelegator,
                         final NamedEntityRecognizer namedEntityRecognizer,
                         final ChatModelRequest queryRewriteRequest) {
        this.chatModelDelegator = chatModelDelegator;
        this.queryRewriteRequest = queryRewriteRequest;
    }

    @SuppressWarnings("uncheck")
    @Override
    public void process(final Map<String, ?> args) {
        String rawQuery = (String) args.get("RawQuery");
        var ners = (Map<String, String>) args.get("NERs");
        var request = this.queryRewriteRequest.withRawQuery(rawQuery).withNERs(ners);
        String results = chatModelDelegator.delegate(request, new TypeReference<List<String>>() {}).getFirst();
        ((Map)args).put("RewritedQuery", results);
    }
}
