package com.github.wordsless.galaxy.core.vectordb;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Milvus 向量数据库实现类，实现 VectorDB 接口
 */
public class MilvusVectorDB implements VectorDB {

    // Milvus 嵌入式存储核心对象
    private final MilvusEmbeddingStore milvusEmbeddingStore;

    // 嵌入模型：用于把 String 转为向量
    private final EmbeddingModel embeddingModel;

    /**
     * 构造函数，初始化 Milvus 连接配置 + 嵌入模型
     * @param client Milvus Client
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @param embeddingModel 嵌入模型（String → 向量）
     */
    public MilvusVectorDB(MilvusServiceClient client, String collectionName, int dimension, EmbeddingModel embeddingModel) {
        // 构建 MilvusEmbeddingStore 实例
        this.milvusEmbeddingStore = MilvusEmbeddingStore.builder()
                .milvusClient(client)
                .collectionName(collectionName)
                .dimension(dimension)
                .build();

        // 注入嵌入模型
        this.embeddingModel = embeddingModel;
    }

    /**
     * 保存向量并返回主键ID
     * @param vectorIndex 浮点型向量数组
     * @return 存储后的主键ID（Milvus 自动生成的ID）
     */
    @Override
    public Long save(float[] vectorIndex) {
        // 将 float 数组转换为 langchain4j 的 Embedding 对象
        Embedding embedding = Embedding.from(vectorIndex);

        // 存储向量（此处无附加文本信息，传入 null 即可）
        String ids = milvusEmbeddingStore.add(embedding);

        // 返回第一个ID（单次存储单个向量时，列表仅含一个元素）
        return Long.parseLong(ids);
    }

    /**
     * 检索与目标向量最相似的向量ID列表
     * @param vectorIndex 查询向量数组
     * @return 相似向量的ID列表
     */
    @Override
    public List<Long> search(float[] vectorIndex) {
        // 将 float 数组转换为 langchain4j 的 Embedding 对象
        Embedding queryEmbedding = Embedding.from(vectorIndex);

        // 构建检索请求
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(10) // 默认返回前10个相似结果，可根据需求调整
                .build();

        // 执行检索
        var matches = milvusEmbeddingStore.search(searchRequest).matches();

        // 提取匹配结果中的向量ID并返回
        return matches.stream()
                .map(match -> Long.parseLong(match.embeddingId()))
                .collect(Collectors.toList());
    }

    /**
     * 根据【文本字符串】直接检索相似向量（已补全）
     * @param query 查询文本
     * @return 相似向量ID列表
     */
    @Override
    public List<Long> search(String query) {
        // 1. 使用嵌入模型把 String 转为向量
        var queryEmbedding = embeddingModel.embed(query);

        // 2. 复用已有的 float[] 检索逻辑
        return search(queryEmbedding.content().vector());
    }

}