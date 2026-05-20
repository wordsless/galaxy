package com.github.wordsless.galaxy.core.algorithm.hiro;

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import java.util.List;
import java.util.Optional;

/**
 * Hiro算法核心实现类
 * 基于树形结构的节点检索/遍历/相似度计算等核心逻辑
 */
public class HiroEngine {

    // 相似度阈值（可调整：根据业务场景优化阈值）
    public static final float SIMILARITY_THRESHOLD = 0.8f;

    /**
     * 核心方法：根据向量相似度检索匹配的HiroTreeNode
     * @param rootNode 树的根节点
     * @param targetVector 目标向量
     * @return 匹配的节点（无则返回空）
     */
    public Optional<HiroTreeNode> searchByVector(TreeNode rootNode, float[] targetVector) {
        // 1. 空值校验（可调整：补充更严格的参数校验）
        if (rootNode == null || targetVector == null || targetVector.length == 0) {
            return Optional.empty();
        }

        // 2. 递归遍历树节点（可调整：改为非递归遍历提升性能；或增加剪枝逻辑）
        return traverseAndMatch((HiroTreeNode) rootNode, targetVector);
    }

    /**
     * 递归遍历节点并计算向量相似度
     * @param currentNode 当前遍历节点
     * @param targetVector 目标向量
     * @return 匹配的HiroTreeNode
     */
    private Optional<HiroTreeNode> traverseAndMatch(HiroTreeNode currentNode, float[] targetVector) {
        // 基例：当前节点不是HiroTreeNode，直接跳过
        if (!(currentNode instanceof HiroTreeNode hiroNode)) {
            return Optional.empty();
        }

        // 3. 向量相似度计算（可调整：替换相似度算法，如余弦相似度/欧氏距离）
        float similarity = calculateVectorSimilarity(hiroNode.getVector(), targetVector);

        // 4. 阈值判断（可调整：改为区间匹配，如[0.7,0.9]）
        if (similarity >= SIMILARITY_THRESHOLD) {
            return Optional.of(hiroNode);
        }

        // 5. 递归遍历子节点（可调整：调整遍历顺序，如深度优先/广度优先；或并行遍历）
        List<TreeNode> children = currentNode.getChildren();
        for (TreeNode child : children) {
            Optional<HiroTreeNode> matchNode = traverseAndMatch((HiroTreeNode) child, targetVector);
            if (matchNode.isPresent()) {
                return matchNode;
            }
        }

        return Optional.empty();
    }

    /**
     * 向量相似度计算（余弦相似度）（可调整：替换为其他相似度算法）
     * @param vector1 节点向量
     * @param vector2 目标向量
     * @return 相似度（0~1）
     */
    public float calculateVectorSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ========== 可扩展/调整的其他核心方法 ==========
    /**
     * 构建Hiro树（可调整：调整树的构建规则，如按深度限制、内容分块规则）
     */
    public HiroTreeNode buildHiroTree(Long rootId, String rootContent, float[] rootVector) {
        HiroTreeNode root = new HiroTreeNode(rootId, 0, null, rootContent, rootVector);
        // 示例：添加子节点（可调整：批量构建/动态构建）
        HiroTreeNode child1 = new HiroTreeNode(1L, 1, root, "子节点1内容", new float[]{0.1f, 0.2f, 0.3f});
        root.getChildren().add(child1);
        return root;
    }

    /**
     * 更新节点向量（可调整：增加向量更新策略，如增量更新/全量更新）
     */
    public boolean updateNodeVector(HiroTreeNode node, float[] newVector) {
        if (node == null || newVector == null) {
            return false;
        }
        node.setVector(newVector);
        return true;
    }
}