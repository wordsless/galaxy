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

public class UCTMetricAccumulator<T> implements MetricAccumulator<T> {

    private final double explorationConstant;

    /**
     * @param explorationConstant 探索常数，通常取 Math.sqrt(2) 或根据问题调节
     */
    public UCTMetricAccumulator(double explorationConstant) {
        this.explorationConstant = explorationConstant;
    }

    @Override
    public double accumulate(TreeNode<T> node) {
        if (!(node instanceof ReasonTreeNode)) {
            throw new IllegalArgumentException("UCTScorer requires MCTSNode");
        }
        ReasonTreeNode<T> reasonTreeNode = (ReasonTreeNode<T>) node;

        int visitCount = reasonTreeNode.getVisitCount();
        if (visitCount == 0) {
            // 未访问节点给予极大值，强制优先探索
            return Double.POSITIVE_INFINITY;
        }

        // 利用项：平均价值
        double exploit = reasonTreeNode.getValueSum() / visitCount;

        // 探索项
        ReasonTreeNode<T> parent = (ReasonTreeNode<T>) reasonTreeNode.getParent();
        if (parent == null) {
            // 根节点没有父节点，仅返回利用项
            return exploit;
        }
        int parentVisitCount = parent.getVisitCount();

        double explore = explorationConstant * Math.sqrt(Math.log(parentVisitCount) / visitCount);

        return exploit + explore;
    }
}