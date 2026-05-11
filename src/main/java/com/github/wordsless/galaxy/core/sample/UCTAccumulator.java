/*
 * MIT License
 * Copyright (c) 2025 Qiang Li (李强)
 *
 * UCT 价值计算器，将选择阶段的 UCT 公式集中在这里。
 * 意图：消除 computeUCT 代码重复，支持配置化探索常数，增加空指针防护。
 */
package com.github.wordsless.galaxy.core.sample;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UCTAccumulator {
    private final MctsConfig mctsConfig;

    /**
     * 计算给定节点的 UCT 值（Upper Confidence Bound for Trees）。
     * @param node 待计算的节点
     * @return UCT 值，未访问节点返回 +∞ 以优先探索，
     *         空节点或异常返回 -∞
     */
    public double accumulate(ReasoningTreeNode<?, ?> node) {
        if (node == null) {
            return Double.NEGATIVE_INFINITY;
        }

        int childVisits = node.getVisitCount();
        // 未访问的节点拥有最高的优先级（鼓励探索）
        if (childVisits == 0) {
            return Double.POSITIVE_INFINITY;
        }

        // 利用项：平均奖励 Q(s,a)
        double exploitation = node.getQValue();

        // 只有非根节点才需要计算探索项（根节点不存在父节点）
        ReasoningTreeNode<?, ?> parent = node.getParent();
        if (parent == null) {
            return exploitation;
        }

        int parentVisits = parent.getVisitCount();
        // 避免父节点访问数为 0 导致的除零错误（实际上父节点至少被访问一次）
        if (parentVisits == 0) {
            return exploitation;
        }

        // 探索项：c * sqrt( ln(N_parent) / N_child )
        double exploration = mctsConfig.getExplorationConstant() *
                Math.sqrt(Math.log(parentVisits) / childVisits);

        return exploitation + exploration;
    }
}