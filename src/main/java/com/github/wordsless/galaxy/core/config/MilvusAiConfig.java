package com.github.wordsless.galaxy.core.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusAiConfig {

    // 1. Milvus 客户端
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(19530)
                        .build()
        );
    }

    // Langchain4j 适配的 Milvus 向量存储
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(MilvusServiceClient milvusClient) {
        return MilvusEmbeddingStore.builder()
                .milvusClient(milvusClient)
                .collectionName("test_vector_store")
                .databaseName("default")
                .dimension(1536) // 根据实际使用的嵌入模型维度调整
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.COSINE)
                .build();
    }
}