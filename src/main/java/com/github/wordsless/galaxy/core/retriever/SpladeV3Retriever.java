package com.github.wordsless.galaxy.core.retriever;

import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.encoder.SpladeV3Encoder;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.param.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpladeV3Retriever implements Retriever {

    private final SpladeV3Encoder spladeV3Encoder;

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private Integer milvusPort;

    @Value("${milvus.collectionName:document_collection}")
    private String collectionName;

    @Value("${milvus.databaseName:default}")
    private String databaseName;

    @Value("${milvus.topK:5}")
    private Integer topK;

    private MilvusEmbeddingStore milvusVectorStore;

    @PostConstruct
    public void initMilvusClient() {
        try {
            milvusVectorStore = MilvusEmbeddingStore.builder()
                    .host(milvusHost)
                    .port(milvusPort)
                    .databaseName(databaseName)
                    .collectionName(collectionName)
                    .metricType(MetricType.IP)
                    .build();

            log.info("Langchain4j Milvus 向量存储初始化成功，集合: {}", collectionName);
        } catch (Exception e) {
            log.error("Langchain4j Milvus 客户端初始化失败", e);
            throw new RuntimeException("Langchain4j Milvus 客户端初始化失败", e);
        }
    }

    @Override
    public List<Document> retrieve(Query rewritedQuery) {
        String queryText = rewritedQuery.getText();
        Embedding queryEmbedding = generateSpladeEmbedding(queryText);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK) // 对应你原来的 topK
                .minScore(0.0)     // 最低相似度，默认0
                .build();

        List<EmbeddingMatch<TextSegment>> matches;
        try {
            // ✅ 适配旧版 API：用 search 方法
            matches = milvusVectorStore.search(request).matches();
        } catch (Exception e) {
            log.error("Langchain4j Milvus 检索失败", e);
            return Collections.emptyList();
        }

        return convertToBusinessDocuments(matches);
    }

    private Embedding generateSpladeEmbedding(String queryText) {
        Map<String, Float> sparseVector = spladeV3Encoder.encode(queryText);
        float[] embeddingArray = new float[sparseVector.size()];
        int index = 0;
        for (float value : sparseVector.values()) {
            embeddingArray[index++] = value;
        }
        return Embedding.from(embeddingArray);
    }

    private List<Document> convertToBusinessDocuments(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> documents = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            // ✅ 适配旧版 API：用 content() 获取 TextSegment
            TextSegment segment = match.embedded();
            Document doc = new Document();

            Metadata metadata = segment.metadata();
            // ✅ 适配旧版 Metadata API：用 getAsString/getAsLong
            String idStr = metadata.getString("id");
            doc.setId(Long.parseLong(idStr));
            doc.setContent(segment.text());
            doc.setMetadata(convertMetadata(metadata));

            documents.add(doc);
        }
        return documents;
    }

    /**
     * 适配旧版 Metadata 的转换方法（无 keys/asMap）
     */
    private Map<String, Object> convertMetadata(Metadata metadata) {
        Map<String, Object> map = new HashMap<>();
        // 手动列出你用到的所有元数据字段，这里示例几个常用的
        // 你可以根据自己的业务补充更多字段
        map.put("id", metadata.getString("id"));
        map.put("source", metadata.getString("source"));
        map.put("title", metadata.getString("title"));
        map.put("timestamp", metadata.getLong("timestamp"));
        return map;
    }
}