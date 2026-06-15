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

import com.github.wordsless.galaxy.core.algorithm.air.Accumulator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Component
public abstract class MCTSEngine<S> {

    private final Accumulator accumulator; // 收益累加器（UCT）
    private final ThreadLocalRandom random;

    private int maxIterCount;

    protected int sampling;

    // 推荐使用 UCTAccumulator 作为默认实现
    public MCTSEngine(int maxIterCount, int sampling) {
        this.accumulator = new UCTAccumulator();
        this.random = ThreadLocalRandom.current();
        this.maxIterCount = maxIterCount;
        this.sampling = sampling;
    }

    @Autowired
    public MCTSEngine(final Accumulator accumulator, final int maxIterCount, final int sampling) {
        this.accumulator = Objects.requireNonNull(accumulator, "Accumulator cannot be null");
        this.random = ThreadLocalRandom.current();
        this.maxIterCount = maxIterCount;
        this.sampling = sampling;
    }

    // MCTS核心迭代（单次迭代：选择→扩展→模拟→回溯）
    public void iterate(MCTSNode<S> root) {
        Objects.requireNonNull(root, "Root node cannot be null");

        for (int c = 0; c < maxIterCount; c++) { // 替换while为for，更直观的迭代计数
            // 1. 选择阶段：从根节点向下选择最优子节点，直到叶子/终止节点
            MCTSNode<S> selectedNode = select(root);

            // ==============================
            // 核心漏洞修复：明确处理终止节点的回溯逻辑
            // 问题：若终止节点不回溯，该路径的访问次数/收益未更新，导致UCT计算偏差
            // 修复：检测到终止节点时，立即回溯并跳过后续扩展/模拟步骤
            // ==============================
            if (isTerminalState(selectedNode)) {
                double terminalReward = getTerminalReward(selectedNode);
                // 回溯更新从终止节点到根节点的所有节点统计信息
                backpropagate(selectedNode, terminalReward);
                continue; // 终止节点无扩展/模拟必要，进入下一轮迭代
            }

            // 2. 扩展阶段：为非终止叶子节点生成子节点
            List<MCTSNode<S>> expandeds = expand(selectedNode);
            if (expandeds.isEmpty()) {
                // 补充：无扩展节点但非终止节点的边界场景（如异常情况），仍需回溯默认收益
                backpropagate(selectedNode, 0.0); // 避免路径完全无更新
                continue;
            }

            // 随机选择一个扩展节点进行模拟（MCTS标准策略）
            MCTSNode<S> expandedNode = expandeds.get(random.nextInt(expandeds.size()));

            // 3. 模拟阶段：从扩展节点随机模拟到终止状态，获取收益
            double simulateReward = simulate(expandedNode);

            // 4. 回溯阶段：更新扩展节点到根节点的访问次数和收益
            backpropagate(expandedNode, simulateReward);
        }
    }

    // 选择阶段：基于UCT值递归选择最优子节点，直到未完全展开/终止节点
    public MCTSNode<S> select(@NotNull MCTSNode<S> node) {
        MCTSNode<S> currentNode = node;
        // 循环条件：节点已完全展开 + 非终止节点 → 继续向下选择
        while (currentNode.hasExpanded() && !isTerminalState(currentNode)) {
            double bestUCT = Double.NEGATIVE_INFINITY;
            MCTSNode<S> bestChild = null;
            for (var child : currentNode.getChildren()) {
                double uctValue = accumulator.accumulate(child);
                if (uctValue > bestUCT) {
                    bestUCT = uctValue;
                    bestChild = child;
                }
            }
            // 补充：防止空指针（如子节点被异常清空）
            if (bestChild == null) {
                break;
            }
            currentNode = bestChild;
        }
        return currentNode;
    }

    // 扩展阶段：为叶子节点生成所有合法子节点，更新节点的子节点列表
    public List<MCTSNode<S>> expand(@NotNull MCTSNode<S> node) {
        // 前置校验：终止节点不允许扩展
        if (isTerminalState(node)) {
            return List.of();
        }
        List<MCTSNode<S>> expansions = onExpanded(node);
        node.getChildren().addAll(expansions);
        return expansions;
    }

    // 模拟阶段：封装业务层的模拟逻辑
    public double simulate(@NotNull MCTSNode<S> node) {
        // 前置校验：终止节点直接返回终止收益，无需模拟
        if (isTerminalState(node)) {
            return getTerminalReward(node);
        }
        return onSimulate(node);
    }

    // 回溯阶段：从目标节点向上更新所有父节点的访问次数和累计收益
    public void backpropagate(@NotNull MCTSNode<S> node, double value) {
        MCTSNode<S> currentNode = node;
        while (currentNode != null) {
            currentNode.increaseVisitCount(); // 访问次数+1
            currentNode.updateSumValue(value); // 累计收益更新
            currentNode = currentNode.getParent(); // 向上遍历父节点
        }
    }

    // ========== 抽象方法：由业务层实现具体逻辑 ==========
    // 生成节点的所有合法后继节点（扩展逻辑）
    public abstract List<MCTSNode<S>> onExpanded(@NotNull MCTSNode<S> node);

    // 模拟到终止状态的业务逻辑（返回模拟收益）
    public abstract double onSimulate(@NotNull MCTSNode<S> node);

    // 判断节点是否为终止状态（如游戏结束、任务完成等）
    public abstract boolean isTerminalState(@NotNull MCTSNode<S> node);

    // 获取终止状态的固定收益（如赢=1.0，输=0.0，平=0.5）
    public abstract double getTerminalReward(@NotNull MCTSNode<S> node);

}