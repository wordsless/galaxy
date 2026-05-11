package com.github.wordsless.galaxy.core.sample;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 LangChain4j + ONNX Runtime 的语义相似度投票器。
 * 纯 Java 实现，无 Python 依赖，模型首次加载自动下载。
 */
public class EmbeddingConsistencyVoter implements ConsistencyVoter {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingConsistencyVoter.class);

    private final EmbeddingModel embeddingModel;
    private final Map<String, Embedding> cache = new ConcurrentHashMap<>();

    /**
     * 使用默认英文/多语言模型（All-MiniLM-L6-v2，量化版）
     * 支持中英文，速度快，内存占用低。
     */
    public EmbeddingConsistencyVoter() {
        // 切换中文模型请将下面一行注释，并取消注释另一行
        this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        // this.embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
        log.info("LangChain4j embedding model initialized: {}", embeddingModel.getClass().getSimpleName());
    }

    /**
     * 允许外部注入自定义 EmbeddingModel（如使用 API 模型）
     */
    public EmbeddingConsistencyVoter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    private Embedding getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Embedding.from(new float[0]);
        }
        return cache.computeIfAbsent(text, t -> {
            try {
                return embeddingModel.embed(t).content();
            } catch (Exception e) {
                log.error("Failed to compute embedding for text: {}", t, e);
                return Embedding.from(new float[0]);
            }
        });
    }

    private double cosineSimilarity(Embedding e1, Embedding e2) {
        if (e1 == null || e2 == null) return 0.0;
        float[] v1 = e1.vector();
        float[] v2 = e2.vector();
        if (v1.length == 0 || v2.length == 0) return 0.0;
        return CosineSimilarity.between(e1, e2);
    }

    @Override
    public String vote(List<String> answers) {
        if (answers == null || answers.isEmpty()) return null;
        if (answers.size() == 1) return answers.get(0);

        Embedding[] embeddings = answers.stream()
                .map(this::getEmbedding)
                .toArray(Embedding[]::new);

        double[] scores = new double[answers.size()];
        for (int i = 0; i < answers.size(); i++) {
            for (int j = i + 1; j < answers.size(); j++) {
                double sim = cosineSimilarity(embeddings[i], embeddings[j]);
                scores[i] += sim;
                scores[j] += sim;
            }
        }

        int bestIdx = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[bestIdx]) bestIdx = i;
        }
        return answers.get(bestIdx);
    }

    /**
     * 清空嵌入缓存（可选，在长时间运行后可调用以释放内存）
     */
    public void clearCache() {
        cache.clear();
    }
}