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
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.utils.SpladeV3;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpladeV3Retriever implements Retriever {

    private final SpladeV3 spladeV3;
    private final VectorStore milvusVectorStore;

    /**
     * 【严格实现】Retriever 接口的 retrieve 方法
     */
    @Override
    public List<Document> retrieve(Query rewritedQuery) {
        // 1. 获取查询文本
        String queryText = rewritedQuery.getText();

        // 2. 生成 SPLADE v3 稀疏向量（自动按 token 截断 512）
        Map<String, Float> sparseVector = spladeV3.encode(queryText);

        // 3. 构建 Milvus 稀疏检索
        SearchRequest searchRequest = SearchRequest.builder()
                                                   .query(queryText)
                                                   .topK(5)
                                                   .build();


        // 4. 执行检索
        List<org.springframework.ai.document.Document> springAiDocs = milvusVectorStore.similaritySearch(searchRequest);

        // 5. 转换为项目的 Document 并返回
        return springAiDocs.stream()
                .map(this::convert)
                .toList();
    }

    /**
     * Spring AI Document → 你的业务 Document
     */
    private Document convert(org.springframework.ai.document.Document aiDoc) {
        Document document = new Document();
        document.setId(Long.parseLong(aiDoc.getId()));
        document.setContent(aiDoc.getText());
        document.setMetadata(aiDoc.getMetadata());
        return document;
    }
}
