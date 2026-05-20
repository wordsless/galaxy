package com.github.wordsless.galaxy.core.orchestrator;

import com.github.wordsless.galaxy.core.algorithm.hiro.HiroEngine;
import com.github.wordsless.galaxy.core.algorithm.hiro.HiroTreeNode;
import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HiroOrchestrator extends AbstractBasicOrchestrator {

    // 上下文 KEY（来自你的框架）
    public static final String KEY_ROOT_NODE    = "rootNode";
    public static final String KEY_RAW_QUERY    = "rawQuery";
    public static final String KEY_QUERY_VECTOR = "queryVector";

    // HIRO 论文双阈值（检索策略章节明确给出）
    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final double DELTA_GAIN_THRESHOLD = 0.1;

    private final HiroEngine hiroEngine = new HiroEngine();

    // ==============================================
    // 🔥 真正实现：严格按照检索策略章节 HIRO 流程
    // ==============================================
    @Override
    public List<String> retrieve(Map<String, ?> context) {
        List<String> output = new ArrayList<>();

        // 1. 从上下文获取
        HiroTreeNode root    = (HiroTreeNode) context.get(KEY_ROOT_NODE);
        float[] queryVector  = (float[]) context.get(KEY_QUERY_VECTOR);
        String rawQuery      = (String) context.get(KEY_RAW_QUERY); // 仅用于日志

        // 2. 必要校验
        if (root == null || queryVector == null) {
            return output;
        }

        // 3. 执行 HIRO 递归检索（完全来自论文）
        dfsPruneRetrieve(root, queryVector, 0.0, output);

        return output;
    }

    /**
     * HIRO 核心：
     * 层次递归检索 + 双阈值剪枝
     * 完全对应综述 3.3 检索策略增强
     */
    private void dfsPruneRetrieve(
            HiroTreeNode currentNode,
            float[] queryVec,
            double parentSim,
            List<String> output
    ) {
        // 计算相似度（使用 HiroEngine 已有方法）
        double sim = hiroEngine.calculateVectorSimilarity(
                currentNode.getVector(), queryVec
        );

        // ================== 论文剪枝规则 1 ==================
        if (sim < SIMILARITY_THRESHOLD) {
            return;
        }

        // ================== 论文剪枝规则 2 ==================
        double delta = sim - parentSim;
        if (delta < DELTA_GAIN_THRESHOLD) {
            output.add(currentNode.getContent());
            return;
        }

        // 叶子节点直接加入
        List<TreeNode> children = currentNode.getChildren();
        if (children == null || children.isEmpty()) {
            output.add(currentNode.getContent());
            return;
        }

        // 递归子节点
        for (TreeNode child : children) {
            dfsPruneRetrieve((HiroTreeNode) child, queryVec, sim, output);
        }
    }
}