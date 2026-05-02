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

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;

public class UCTAccumulator<T> implements MetricAccumulator<T> {

    private static final double EXPLORATION_CONSTANT = Math.sqrt(2);

    @Override
    public double accumulate(TreeNode<T> node) {
        var child = (ReasonTreeNode<T>) node;
        int childVisits = child.getVisitCount();
        if (childVisits == 0) {
            return Double.POSITIVE_INFINITY; // 未访问节点优先探索
        }
        double exploitation = child.getValueSum() / childVisits; // Q(s,a)/N(s)
        ReasonTreeNode<?> parent = (ReasonTreeNode<?>) child.getParent();
        int parentVisits = parent.getVisitCount();
        double exploration = EXPLORATION_CONSTANT * Math.sqrt(Math.log(parentVisits) / childVisits);
        return exploitation + exploration;
    }
}
