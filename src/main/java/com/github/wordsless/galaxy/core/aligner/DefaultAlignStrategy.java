package com.github.wordsless.galaxy.core.aligner;

import com.github.wordsless.galaxy.core.Aligner;
import com.github.wordsless.galaxy.core.Scorer;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.utils.MonteCarloSampler;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 检索后增强 - 文档对齐处理器
 * 核心职责：多轮迭代对齐 + 智能采样 + 多维度评分 + 低质过滤，输出高质量文档集
 * 设计定位：RAG 检索后增强核心组件（对齐→重排→过滤）
 */
@Component
public class DefaultAlignStrategy implements AlignStrategy {

    private final MonteCarloSampler<Document> sampler;
    private final Aligner aligner;
    private final List<Scorer> scorers;

    @Value("token_count_limit")
    private long tokenCountLimit;

    @Value("iterate_count")
    private int iterCount;

    @Value("threshold")
    private double threshold;

    public DefaultAlignStrategy(@NonNull final Aligner aligner,
                                final MonteCarloSampler<Document> sampler,
                                final List<Scorer> scorers) {
        this.sampler = sampler;
        this.aligner = aligner;
        this.scorers = CollectionUtils.isEmpty(scorers) ? Collections.emptyList() : scorers;
    }

    /**
     * 文档对齐主流程
     * @param docs 原始检索文档
     * @return 最终高质量文档
     */
    @Override
    public List<Document> align(List<Document> docs) {
        // 1. 入参基础校验
        if (CollectionUtils.isEmpty(docs)) {
            return Collections.emptyList();
        }
        if (iterCount <= 0 || tokenCountLimit <= 0) {
            return docs;
        }

        // 文档索引：保证迭代过程中文档可替换、可更新
        Map<Long, Document> docIndex = new HashMap<>(docs.size());
        docs.forEach(doc -> docIndex.put(doc.getId(), doc));

        // 2. 多轮迭代对齐 + 分数收集
        Map<Long, Map<String, List<Double>>> multiRoundScores = new HashMap<>();
        int currentIter = 0;

        while (currentIter < iterCount) {
            // 采样：控制单次处理长度，防止上下文溢出
            // 调整：使用新的Sampler接口，tokenCounter返回Long
            List<Document> subset = null;
            if(sampler == null)
                subset = docs;
            else
                sampler.sample(new ArrayList<>(docIndex.values()),
                                               tokenCountLimit,
                                               Document::getTokenCount);

            if (CollectionUtils.isEmpty(subset)) {
                break;
            }

            // 对齐：核心优化逻辑（内容修正/增强/结构化）
            Map<Document, Map<String, Double>> alignedResult = aligner.align(subset);

            // 同步更新文档 + 收集每一轮分数
            for (Map.Entry<Document, Map<String, Double>> entry : alignedResult.entrySet()) {
                Document alignedDoc = entry.getKey();
                Map<String, Double> scoreItem = entry.getValue();

                // 更新文档索引
                if (alignedDoc != null) {
                    docIndex.put(alignedDoc.getId(), alignedDoc);
                }

                // 累计多轮分数
                Long docId = alignedDoc.getId();
                for (Map.Entry<String, Double> scoreEntry : scoreItem.entrySet()) {
                    String label = scoreEntry.getKey();
                    Double score = scoreEntry.getValue();

                    multiRoundScores
                            .computeIfAbsent(docId, k -> new HashMap<>())
                            .computeIfAbsent(label, k -> new ArrayList<>())
                            .add(score);
                }
            }
            currentIter++;
        }

        // 3. 计算平均分
        Map<Long, Map<String, Double>> avgScores = calculateAverageScores(multiRoundScores);

        // 4. 多评分器综合打分 + 阈值过滤
        filterLowQualityDocs(docIndex, avgScores, threshold);

        // 输出结果
        return new ArrayList<>(docIndex.values());
    }

    /**
     * 计算多轮迭代后的平均分
     */
    private Map<Long, Map<String, Double>> calculateAverageScores(
            Map<Long, Map<String, List<Double>>> multiRoundScores) {
        Map<Long, Map<String, Double>> avgScores = new HashMap<>();

        multiRoundScores.forEach((docId, labelScores) -> {
            Map<String, Double> avgItem = new HashMap<>();
            labelScores.forEach((label, scores) -> {
                double average = scores.stream()
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0d);
                avgItem.put(label, average);
            });
            avgScores.put(docId, avgItem);
        });
        return avgScores;
    }

    /**
     * 多评分器过滤：【任意一个 scorer 低于阈值 → 标记删除】
     */
    private void filterLowQualityDocs(
            Map<Long, Document> docIndex,
            Map<Long, Map<String, Double>> avgScores,
            double threshold) {
        if (CollectionUtils.isEmpty(scorers)) {
            return;
        }

        Set<Long> needRemoveDocIds = new HashSet<>();

        avgScores.forEach((docId, itemizedScores) -> {
            boolean lowQuality = scorers.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(scorer -> scorer.score(itemizedScores) < threshold);

            if (lowQuality) {
                needRemoveDocIds.add(docId);
            }
        });

        // 批量移除低质文档
        needRemoveDocIds.forEach(docIndex::remove);
    }
}