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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReasoningEngine {

    private static final double EXPLORATION_CONSTANT = Math.sqrt(2);

    private ReasoningTreeNode<Object, String> root;
    private final ReasoningTreeNodeFactory factory;
    private final Simulator simulator;
    private final ConsistencyVoter voter;
    private final int sampling;
    private final int maxDepth;
    private final int minDepth;
    private final int maxIterCount;
    private final Accumulator accumulator;

    private PlanStep planStep;

    @Autowired
    public ReasoningEngine(ReasoningTreeNodeFactory factory,
                           Simulator simulator,
                           Accumulator accumulator,
                           ConsistencyVoter voter,
                           int sampling,
                           int maxDepth,
                           int minDepth,
                           int maxIterCount) {
        this.factory = factory;
        this.simulator = simulator;
        this.accumulator = accumulator;
        this.voter = voter;
        this.sampling = sampling;
        this.maxDepth = maxDepth;
        this.minDepth = minDepth;
        this.maxIterCount = maxIterCount;
    }

    // 选择：若当前节点未完全扩展，返回自身；否则选择 UCT 最大子节点并递归
    private ReasoningTreeNode<?, ?> select(ReasoningTreeNode<?, ?> node) {
        if (node == null)
            return null;
        // if planStep child node of current node is null, it means the current node has completely expanded.
        var planStep = node.getPlanStep();
        if(planStep == null)
            return node;
        var candidate = planStep.next();
        if(candidate != null) {
            // if candidate is not null, it means MCTS need expand current node first.
            return node;
        }
        ReasoningTreeNode<?, ?> bestChild = null;
        double bestUCT = Double.NEGATIVE_INFINITY;
        for (var child : node.getChildren()) {
            double uct = this.accumulator.accumulate(child); //computeUCT(child);
            if (uct > bestUCT) {
                bestUCT = uct;
                bestChild = child;
            }
        }
        if (bestChild == null) {
            return node;
        }
        return select(bestChild);
    }

    // 扩展：从候选动作中取出一个，创建 sampling 个子节点（通常 sampling=1 即可）
    public List<ReasoningTreeNode<?, ?>> expand(final ReasoningTreeNode<?, ?> root, final int sampling) {
        var planStep = root.getPlanStep();
        // 如果当前根节点没有planStep则表示已经没有候选节点了。
        if(planStep == null) {
            return List.of();
        }
        PlanStep candidate = planStep.next();
        var leaves = new ArrayList<ReasoningTreeNode<?, ?>>();
        /*
        var plans = planStep.getChildren();
        for(var plan : plans) {
            if(!plan.isUsed()) {
                candidate = plan;
                break;
            }
        }
        */
        candidate.setUsed(true);
        for(int i = 0; i < sampling; ++i) {
            ReasoningTreeNode<?, ?> child = factory.createNode(candidate, root);
            /*
            var children = root.getChildren();
            for(var sibling : children) {
                child.addSibling(sibling);
                sibling.addSibling(child);
            }
            */
            root.addChild(child);
            leaves.add(child);
        }
        return leaves;
    }

    private double simulate(ReasoningTreeNode<?, ?> node, int minDepth, int maxDepth) {
        return simulator.simulate(node, minDepth, maxDepth);
    }

    private void backpropagate(ReasoningTreeNode<?, ?> node, double reward) {
        ReasoningTreeNode<?, ?> current = node;
        while (current != null) {
            current.increaseVisitCount();
            current.updateValueSum(reward);
            current = current.getParent();
        }
    }

    public void iterate(int maxIterations) {
        for (int i = 0; i < maxIterations; i++) {
            ReasoningTreeNode<?, ?> selected = select(root);
            if (selected == null) continue;
            List<ReasoningTreeNode<?, ?>> expanded = expand(selected, sampling);
            if (expanded.isEmpty()) continue;
            for (var child : expanded) {
                double reward = simulate(child, minDepth, maxDepth);
                backpropagate(child, reward);
            }
        }
    }

    public List<String> collectAnswers(final ReasoningTreeNode<?, ?> root, List<String> collector) {
        if(collector == null)
            collector = new ArrayList<String>();
        var children = root.getChildren();
        if(children.isEmpty() && root.getAnswer() != null) {
            collector.add(root.getAnswer().toString());
            return collector;
        } else {
            for(var child : children) {
                collectAnswers(child, collector);
            }
            return collector;
        }
    }

    public String getAnswer(final String rawQuery) {
        this.root = factory.createRoot(rawQuery);
        this.iterate(this.maxIterCount);
        var answers = this.collectAnswers(this.root, List.of());
        return this.voter.vote(answers);
    }
}