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

package pub.rag.core.algorithm.mcts;

import pub.rag.core.algorithm.tree.TreeNode;
import pub.rag.core.response.SystemAnalysisResponse;

import java.util.List;

public class ReasoningTreeTraverser<T> {

    private MetricAccumulator<T> metricAccumulator;

    private ReasonTreeNodeFactory factory;

    private String rawQuery;

    private String payload;

    private List<String> history;

    public ReasoningTreeTraverser(final String rawQuery,
                                  final MetricAccumulator<T> metricAccumulator,
                                  final ReasonTreeNodeFactory factory) {
        this.rawQuery = rawQuery;
        this.metricAccumulator = metricAccumulator;
        this.factory = factory;
    }

    public void iterate(final ReasonTreeNode<T> root, final int maxIterNum) {
        for (int iter = 0; iter < maxIterNum; iter++) {
            // 1. Selection: 从根开始，使用 UCT 向下走到未完全扩展的节点
            ReasonTreeNode<T> node = select(root);

            // 2. Expansion: 在选中的节点上扩展一个新子节点
            ReasonTreeNode<T> newNode = expand(node);
            if (newNode == null) {
                // 无法扩展（所有动作都已尝试），则直接对该节点模拟（视为叶子）
                newNode = node;
            }

            // 3. Simulation: 从新节点开始快速推演得到奖励值
            double reward = simulate(newNode);

            // 4. Backpropagation: 沿父链向上更新 visitCount 和 valueSum
            backpropagate(newNode, reward);
        }
    }

    /**
     * 从根节点开始，递归选择 UCT 最大的子节点，直到遇到未完全扩展或终止的节点。
     */
    private ReasonTreeNode<?> select(ReasonTreeNode<?> root) {
        ReasonTreeNode<?> node = root;
        while (!node.isLeaf() && node.hasCompletelyExpanded()) {
            node = bestChild(node);
        }
        return node;
    }

    /**
     * 在当前节点的子节点中选择 UCT 最大的一个。
     */
    private ReasonTreeNode<?> bestChild(ReasonTreeNode<?> node) {
        ReasonTreeNode<?> best = null;
        double bestUCT = Double.NEGATIVE_INFINITY;
        for (var child : node.getChildren()) {
            var mctsChild = (ReasonTreeNode<?>) child;
            double uct = metricAccumulator.accumulate(mctsChild);
            if (uct > bestUCT) {
                bestUCT = uct;
                best = mctsChild;
            }
        }
        return best;
    }

    /** 扩展节点：根据当前状态生成一个新子节点（例如选择某个未尝试的 ReasoningAction） */
    private void expand(final ReasonTreeNode<T> root, final int sampling, final Simulator simulator) {
        // if node has completely expanded, just return.
        if(root.hasCompletelyExpanded())
            return;
        var expected = root.nextAction(true);
        var c = 0;
        while(c < sampling) {
            var node = switch (expected) {
                case SAY -> factory.createSAY(root, this.rawQuery, this.payload, this.history);
                case QT  -> factory.createSAY(root, this.rawQuery, this.payload, this.history);
                case RA  -> factory.createSAY(root, this.rawQuery, this.payload, this.history);
                case DA  -> factory.createDA(root, this.rawQuery, this.payload, this.history);
                case SA  -> factory.createSAY(root, this.rawQuery, this.payload, this.history);
                case NONE -> null;
            };
            var children = root.getChildren();
            for(var child : children) {
                node.addSibling(child);
                child.addSibling(node);
            }
            root.addChild(node);
            var confidence = simulator.simulate(node);

            c++;
        }

    }

    /** 模拟：对新节点进行快速推演，返回奖励值（例如调用 LLM 生成答案并打分） */
    private double simulate(ReasonTreeNode<T> node) {
        return simulator.simulate(node);
    }

    /** 回溯：从给定节点向上更新所有祖先节点的 visitCount 和 valueSum */
    private void backpropagate(ReasonTreeNode<T> node) {
        ReasonTreeNode<T> current = node;
        while (current != null) {
            // 注意：当前节点的 visitCount 和 valueSum 已经在模拟阶段更新过
            // 这里只需要更新父节点，但为了避免重复更新，我们可以选择从父节点开始累加子节点的统计值
            // 更常见的做法是在回溯时直接累加奖励，而不是模拟时更新
            // 为了简单，我们在模拟时已经更新了节点自身的统计，回溯时只更新父节点
            ReasonTreeNode<T> parent = (ReasonTreeNode<T>) current.getParent();
            if (parent != null) {
                // 重新计算父节点的 valueSum 和 visitCount（累加所有子节点）
                double sum = 0;
                int count = 0;
                for (TreeNode<T> child : parent.getChildren()) {
                    ReasonTreeNode<T> mctsChild = (ReasonTreeNode<T>) child;
                    sum += mctsChild.getValueSum();
                    count += mctsChild.getVisitCount();
                }
                parent.setValueSum(sum);
                parent.setVisitCount(count);
            }
            current = parent;
        }
    }
}
