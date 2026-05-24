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
import com.github.wordsless.galaxy.core.vectordb.DocsEngine;
import com.github.wordsless.galaxy.core.vectordb.VectorDatabase;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sparse Retriever Implementation
 *
 * Role:
 * Traditional sparse retrieval based on lexical matching and sparse embeddings.
 *
 * Working Method:
 * 1. Generate sparse embedding for the query.
 * 2. Search in sparse vector index.
 * 3. Return top-K relevant documents.
 */
@Slf4j
@Component
public class SparseRetriever implements Retriever {

    private final String collectionName;
    private final VectorDatabase vectorDatabase;
    private final EmbeddingModel embeddingModel;
    private final DocsEngine engine;
    private final int topK;

    public SparseRetriever(String collectionName,
                           VectorDatabase vectorDatabase,
                           EmbeddingModel embeddingModel,
                           DocsEngine engine,
                           int topK) {
        this.collectionName = collectionName;
        this.vectorDatabase = vectorDatabase;
        this.embeddingModel = embeddingModel;
        this.engine = engine;
        this.topK = topK;
    }

    @Override
    public List<String> retrieve(String rewrittenQuery) {
        try {
            if (rewrittenQuery == null || rewrittenQuery.isBlank()) {
                log.warn("SparseRetriever: query is empty");
                return Collections.emptyList();
            }
            if (topK <= 0) {
                log.warn("SparseRetriever: invalid topK: {}", topK);
                return Collections.emptyList();
            }

            // Sparse embedding
            float[] vector = embeddingModel.embed(rewrittenQuery).content().vector();
            if (vector == null || vector.length == 0) {
                log.error("SparseRetriever: embedding generation failed");
                return Collections.emptyList();
            }

            // Search
            var docList = vectorDatabase.search(collectionName, vector, topK);

            // Map to content
            return docList.stream()
                    .map(doc -> {
                        try {
                            return engine.getDocumentById(doc.getId());
                        } catch (Exception e) {
                            log.error("SparseRetriever: get document failed, id: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("SparseRetriever: retrieval failed", e);
            return Collections.emptyList();
        }
    }
}
