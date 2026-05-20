package com.github.wordsless.galaxy.core.orchestrator;

import com.github.wordsless.galaxy.core.algorithm.sire.SiReEngine;
import com.github.wordsless.galaxy.core.algorithm.sire.SimilarNode;
import com.github.wordsless.galaxy.core.algorithm.sire.RelatedNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiReRAGOrchestrator extends AbstractBasicOrchestrator {

    public static final String KEY_QUERY_VECTOR = "queryVector";
    public static final String KEY_SIMILAR_ROOT = "similarTreeRoot";
    public static final String KEY_RELATED_ROOT = "relatedTreeRoot";

    private final SiReEngine engine = new SiReEngine();

    @Override
    public List<String> retrieve(Map<String, ?> context) {
        List<String> fusedResult = new ArrayList<>();

        // 1. 获取参数
        float[] queryVec = (float[]) context.get(KEY_QUERY_VECTOR);
        SimilarNode similarRoot = (SimilarNode) context.get(KEY_SIMILAR_ROOT);
        RelatedNode relatedRoot = (RelatedNode) context.get(KEY_RELATED_ROOT);

        if (queryVec == null || similarRoot == null || relatedRoot == null) {
            return fusedResult;
        }

        // ======================
        // 双路分别检索（活动图）
        // ======================
        List<String> similarResults = engine.searchSimilar(similarRoot, queryVec);
        List<String> relatedResults = engine.searchRelated(relatedRoot, queryVec);

        // ======================
        // 汇总融合（组件图）
        // ======================
        fusedResult.addAll(similarResults);
        fusedResult.addAll(relatedResults);

        return fusedResult;
    }
}