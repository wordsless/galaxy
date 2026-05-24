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

import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.utils.MicroServiceCaller;
import com.github.wordsless.galaxy.core.vectordb.DocsEngine;
import com.github.wordsless.galaxy.core.vectordb.VectorDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dense Retriever Implementation
 *
 * Role:
 * Performs dense retrieval based on BERT sentence embeddings and vector database search.
 * Supports standard dense retrieval algorithms: DPR, BGE, E5, Contriever, ANCE, etc.
 *
 * Working Method:
 * 1. Encode user query into a dense vector using a remote BERT service.
 * 2. Perform ANN (Approximate Nearest Neighbor) search in vector database.
 * 3. Return top-K relevant document IDs and fetch full text from DocsEngine.
 *
 * Output: List of relevant document contents (same as generative/sparse retrievers).
 */
@Slf4j
@Component
public class DenseRetriever implements Retriever {

    private final String collectionName;
    private final MicroServiceCaller caller;
    private final VectorDatabase vectorDatabase;
    private final DocsEngine engine;
    private final int topK;
    private final String algorithmType;

    public DenseRetriever(String collectionName,
                          MicroServiceCaller caller,
                          VectorDatabase vectorDatabase,
                          DocsEngine engine,
                          int topK,
                          String algorithmType) {
        this.collectionName = collectionName;
        this.caller = caller;
        this.vectorDatabase = vectorDatabase;
        this.engine = engine;
        this.topK = topK;
        this.algorithmType = algorithmType;
    }

    @Override
    public List<String> retrieve(String rewrittenQuery) {
        try {
            // Parameter validation
            if (rewrittenQuery == null || rewrittenQuery.isBlank()) {
                log.warn("DenseRetriever: query is empty, return empty list");
                return Collections.emptyList();
            }
            if (topK <= 0) {
                log.warn("DenseRetriever: topK [{}] is invalid", topK);
                return Collections.emptyList();
            }

            // Encode query to dense vector
            float[] queryVector = caller.queryEncode(algorithmType, rewrittenQuery);
            if (queryVector == null || queryVector.length == 0) {
                log.error("DenseRetriever: query embedding is null or empty");
                return Collections.emptyList();
            }

            // ANN search in vector database
            var docIdList = vectorDatabase.search(collectionName, queryVector, topK);

            // Get real document content
            return docIdList.stream()
                    .map(docId -> {
                        try {
                            return engine.getDocumentById(docId.getId());
                        } catch (Exception e) {
                            log.error("DenseRetriever: failed to get document by id: {}", docId.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("DenseRetriever: retrieval failed", e);
            return Collections.emptyList();
        }
    }
}