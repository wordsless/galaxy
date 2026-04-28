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

import lombok.NonNull;
import pub.rag.core.algorithm.tree.AbstractVisitor;
import pub.rag.core.algorithm.tree.TreeNode;

import java.util.List;
import java.util.ArrayList;

public class MctsVisitor<T> extends AbstractVisitor<T> {

    public enum State {
        FIND_LEAF, SELECTION, EXPANSION, SIMULATION, BACKPROPAGATION
    }

    // 假设存在一个 UCT 打分器，用于选择阶段
    private final UCTScorer<T> scorer = new UCTScorer<>(Math.sqrt(2));

    // 假设存在一个模拟器，用于给新节点打分（例如调用 LLM 快速生成答案并评估）
    private final Simulator<T> simulator;  // 需要外部注入

    // 假设存在一个扩展器，用于根据动作生成子节点（例如调用 LLM 执行检索或总结）
    private final Expander<T> expander;    // 需要外部注入

    public MctsVisitor(Simulator<T> simulator, Expander<T> expander) {
        this.simulator = simulator;
        this.expander = expander;
    }

    // 修正 nextState：去除重复的 if(node.isSimulated())
    public State nextState(@NonNull MCTSNode<T> node) {
        if (node.isLeaf()) {
            if (node.isBackpropagated()) {
                return State.EXPANSION;          // 已回溯 → 可以扩展
            } else {
                if (node.isSimulated()) {
                    return State.BACKPROPAGATION; // 已模拟但未回溯 → 回溯
                } else {
                    return State.SIMULATION;      // 未模拟 → 模拟
                }
            }
        } else {
            return State.FIND_LEAF;               // 非叶子 → 需要继续向下找叶子
        }
    }

    @Override
    public void visit(TreeNode<T> node) {
        if (!(node instanceof MCTSNode)) {
            return;
        }
        MCTSNode<T> root = (MCTSNode<T>) node;

        // ========== 1. 选择 (Selection) ==========
        MCTSNode<T> leaf = selectLeaf(root);
        if (leaf == null) return;

        // ========== 2. 扩展 (Expansion) ==========
        // 如果叶子节点尚未完全扩展，则扩展一个新子节点
        MCTSNode<T> newNode = expand(leaf);
        if (newNode == null) {
            // 无法扩展（例如已经达到最大深度或所有动作都已尝试过），直接模拟当前叶子
            newNode = leaf;
        }

        // ========== 3. 模拟 (Simulation) ==========
        double reward = simulate(newNode);
        newNode.setSimulated(true);
        newNode.setValueSum(newNode.getValueSum() + reward);
        newNode.setVisitCount(newNode.getVisitCount() + 1);

        // ========== 4. 回溯 (Backpropagation) ==========
        backpropagate(newNode);

        // 标记回溯完成
        newNode.setBackpropagated(true);
    }

    /** 从根节点开始，按 UCT 分数递归选择子节点，直到叶子节点 */
    private MCTSNode<T> selectLeaf(MCTSNode<T> root) {
        MCTSNode<T> current = root;
        while (!current.isLeaf()) {
            List<TreeNode<T>> children = current.getChildren();
            if (children == null || children.isEmpty()) {
                break;
            }
            // 选出 UCT 分数最高的子节点
            MCTSNode<T> best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (TreeNode<T> child : children) {
                MCTSNode<T> mctsChild = (MCTSNode<T>) child;
                double score = scorer.score(mctsChild);
                if (score > bestScore) {
                    bestScore = score;
                    best = mctsChild;
                }
            }
            if (best == null) break;
            current = best;
        }
        return current;
    }

    /** 扩展节点：根据当前状态生成一个新子节点（例如选择某个未尝试的 ReasoningAction） */
    private MCTSNode<T> expand(MCTSNode<T> node) {
        // 获取所有可用的推理动作（这里简单返回全部，实际应该根据当前上下文过滤）
        ReasoningAction[] allActions = ReasoningAction.values();
        List<ReasoningAction> triedActions = new ArrayList<>();
        if (node.getChildren() != null) {
            for (TreeNode<T> child : node.getChildren()) {
                MCTSNode<T> mctsChild = (MCTSNode<T>) child;
                triedActions.add(mctsChild.getAction());
            }
        }

        // 寻找一个尚未尝试的动作
        ReasoningAction selectedAction = null;
        for (ReasoningAction action : allActions) {
            if (!triedActions.contains(action)) {
                selectedAction = action;
                break;
            }
        }
        if (selectedAction == null) {
            return null; // 所有动作都已尝试，无法扩展
        }

        // 调用外部扩展器生成新节点（例如执行 RA 检索文档，或 QT 重写查询）
        MCTSNode<T> newNode = expander.expand(node, selectedAction);
        if (newNode == null) return null;

        // 将新节点添加到当前节点的子节点列表中
        if (node.getChildren() == null) {
            node.setChildren(new ArrayList<>());
        }
        node.getChildren().add(newNode);
        return newNode;
    }

    /** 模拟：对新节点进行快速推演，返回奖励值（例如调用 LLM 生成答案并打分） */
    private double simulate(MCTSNode<T> node) {
        return simulator.simulate(node);
    }

    /** 回溯：从给定节点向上更新所有祖先节点的 visitCount 和 valueSum */
    private void backpropagate(MCTSNode<T> node) {
        MCTSNode<T> current = node;
        while (current != null) {
            // 注意：当前节点的 visitCount 和 valueSum 已经在模拟阶段更新过
            // 这里只需要更新父节点，但为了避免重复更新，我们可以选择从父节点开始累加子节点的统计值
            // 更常见的做法是在回溯时直接累加奖励，而不是模拟时更新
            // 为了简单，我们在模拟时已经更新了节点自身的统计，回溯时只更新父节点
            MCTSNode<T> parent = (MCTSNode<T>) current.getParent();
            if (parent != null) {
                // 重新计算父节点的 valueSum 和 visitCount（累加所有子节点）
                double sum = 0;
                int count = 0;
                for (TreeNode<T> child : parent.getChildren()) {
                    MCTSNode<T> mctsChild = (MCTSNode<T>) child;
                    sum += mctsChild.getValueSum();
                    count += mctsChild.getVisitCount();
                }
                parent.setValueSum(sum);
                parent.setVisitCount(count);
            }
            current = parent;
        }
    }

    // 以下为辅助接口定义（实际使用时需要外部实现）
    public interface Simulator<T> {
        double simulate(MCTSNode<T> node);
    }

    public interface Expander<T> {
        MCTSNode<T> expand(MCTSNode<T> parent, ReasoningAction action);
    }
}