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

/**
 * UCT（Upper Confidence Bound for Trees）累加器实现
 * 核心公式：UCT = (节点收益/访问次数) + C * sqrt(ln(父节点访问次数)/节点访问次数)
 * C 为探索系数，通常取 sqrt(2)
 */
public class UCTAccumulator implements Accumulator {

    // 探索系数（平衡探索与利用）
    private static final double EXPLORATION_CONSTANT = Math.sqrt(2);

    @Override
    public double accumulate(MCTSNode<?> node) {
        // 未被访问过的节点优先选择（保证探索性）
        if (node.getVisitCount() == 0) {
            return Double.POSITIVE_INFINITY;
        }

        MCTSNode<?> parent = node.getParent();
        if (parent == null || parent.getVisitCount() == 0) {
            return 0.0;
        }

        // UCT 核心公式
        double exploitation = node.getSumValue() / node.getVisitCount(); // 利用（当前节点收益）
        double exploration = EXPLORATION_CONSTANT * Math.sqrt(Math.log(parent.getVisitCount()) / node.getVisitCount()); // 探索（未探索节点的奖励）
        return exploitation + exploration;
    }
}