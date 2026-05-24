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

package com.github.wordsless.galaxy.core.retriever;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.vectordb.DocsEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generative Retriever Implementation
 *
 * Role:
 * Uses large language models (LLM) to directly generate relevant document IDs
 * without embedding or vector search.
 *
 * Working Method:
 * 1. Send query to LLM with a structured prompt.
 * 2. LLM outputs a list of document IDs (docid list).
 * 3. Retrieve full document content from DocsEngine by ID.
 *
 * Output: List of relevant document contents (same as dense/sparse retrievers).
 */
@Slf4j
@Component
public class GenerativeRetriever implements Retriever {

    private final ChatModelDelegator<List<Long>> chatModelDelegator;
    private final ChatModelRequest template;
    private final DocsEngine engine;

    public GenerativeRetriever(ChatModelDelegator<List<Long>> chatModelDelegator,
                               ChatModelRequest template,
                               DocsEngine engine) {
        this.chatModelDelegator = chatModelDelegator;
        this.template = template;
        this.engine = engine;
    }

    @Override
    public List<String> retrieve(String rewrittenQuery) {
        try {
            if (rewrittenQuery == null || rewrittenQuery.isBlank()) {
                log.warn("GenerativeRetriever: query is empty");
                return Collections.emptyList();
            }

            // Build LLM request
            var request = template.withRawQuery(rewrittenQuery);

            // LLM generates document IDs
            List<Long> docIds = chatModelDelegator.delegate(request, new TypeReference<>() {});
            if (docIds == null || docIds.isEmpty()) {
                log.info("GenerativeRetriever: no document IDs returned from LLM");
                return Collections.emptyList();
            }

            // Get real content
            return docIds.stream()
                    .map(id -> {
                        try {
                            return engine.getDocumentById(id);
                        } catch (Exception e) {
                            log.error("GenerativeRetriever: failed to get document: {}", id, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("GenerativeRetriever: retrieval failed", e);
            return Collections.emptyList();
        }
    }
}
