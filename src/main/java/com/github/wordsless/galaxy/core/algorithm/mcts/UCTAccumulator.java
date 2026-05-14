/*
 * MIT License
 *
 * Copyright (c) ${YEAR} Qiang Li (李强)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.wordsless.galaxy.core.algorithm.mcts;

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