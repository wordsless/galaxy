/*
 * MIT License
 *
 * Copyright (c) 2025 Qiang Li (李强)
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
import com.github.wordsless.galaxy.core.Encoder;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Langchain4j EmbeddingStore 实现的稠密检索器
 * 适配所有版本API，修复所有编译错误
 */
@Slf4j
@Component
public class DenseRetriever implements Retriever {

    private final Encoder<float[]> questionEncoder;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Value("${galaxy.retriever.topK:10}")
    private int topK;

    @Value("${galaxy.retriever.scoreThreshold:0.0}")
    private double scoreThreshold;

    public DenseRetriever(final Encoder<float[]> questionEncoder,
                          final EmbeddingStore<TextSegment> embeddingStore) {
        this.questionEncoder = questionEncoder;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public List<Document> retrieve(Query rewritedQuery) {
        try {
            // 1. 入参校验
            if (rewritedQuery == null || rewritedQuery.getText() == null || rewritedQuery.getText().isBlank()) {
                log.warn("DenseRetriever retrieve failed: query text is empty");
                return List.of();
            }
            String queryText = rewritedQuery.getText();
            log.info("Start dense retrieve for query: {}", queryText);

            // 2. 查询文本向量化
            float[] vector = questionEncoder.encode(queryText);
            Embedding queryEmbedding = Embedding.from(vector);

            // 3. 执行向量库近邻搜索（Langchain4j 通用API，兼容新旧版本）
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .minScore(scoreThreshold)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

            // 4. 转换结果为自定义 Document
            return matches.stream()
                    .map(this::convertToCustomDocument)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("DenseRetriever retrieve error", e);
            return List.of();
        }
    }

    /**
     * 将 Langchain4j EmbeddingMatch 转换为自定义 Document
     * 修复了 id() 和 setScore 不存在的问题
     */
    private Document convertToCustomDocument(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Document document = new Document();

        // --- 1. 处理ID（适配TextSegment无id()方法的情况）---
        // 方案A：从Metadata里取ID（推荐，你建库时把ID存在Metadata里）
        String docId = segment.metadata().getString("id");
        if (docId == null) {
            // 方案B：如果Metadata里没有，就用null或生成UUID（根据你的业务调整）
            docId = java.util.UUID.randomUUID().toString();
        }
        document.setId(Long.parseLong(docId));

        // --- 2. 处理文本内容 ---
        document.setContent(segment.text());

        // --- 3. 处理相似度分数（适配你的Document没有setScore的情况）---
        // 方案A：如果你的Document有setScore方法，直接用
        // document.setScore(match.score());

        // 方案B：如果没有setScore，就把分数存到Metadata里（兼容所有情况）
        Map<String, Object> metadataMap = segment.metadata().toMap();
        metadataMap.put("score", match.score());
        document.setMetadata(metadataMap);

        return document;
    }
}