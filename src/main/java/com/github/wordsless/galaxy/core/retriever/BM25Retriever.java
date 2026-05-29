package com.github.wordsless.galaxy.core.retriever;

import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BM25Retriever implements Retriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    // 注入 EmbeddingStore 和 EmbeddingModel（和之前一样）
    public BM25Retriever(EmbeddingStore<TextSegment> embeddingStore,
                         EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Document> retrieve(Query rewrittenQuery) {
        AtomicLong sn = new AtomicLong(0);
        List<Document> docs = new ArrayList<>();

        // 1. 生成查询向量
        Embedding queryEmbedding = embeddingModel.embed(rewrittenQuery.getText()).content();

        // 2. 用 1.14.x 标准方式构建 EmbeddingSearchRequest
        int maxResults = 10;
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build();

        // 3. 执行搜索（1.14.x 唯一正确的 search 调用方式）
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        // 4. 转换为你的自定义 Document（修正构造方法参数）
        matches.forEach(match -> {
            TextSegment segment = match.embedded();
            // 这里按你报错提示的构造方法：Document(Long, Long, String, Map<String,?>)
            Document doc = new Document(
                    sn.getAndIncrement(),
                    0L, // 第二个 Long 参数，按你原代码的 0L 传入
                    segment.text(),
                    segment.metadata().toMap() // Metadata 转 Map
            );
            docs.add(doc);
        });

        return docs;
    }
}