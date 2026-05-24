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

package com.github.wordsless.galaxy.core.algorithm.air;

import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSEngine;
import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSNode;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReasonEngine extends MCTSEngine<ReasoningState<?>> {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private static final int MAX_ROLLOUT_DEPTH = 10;

    // 构造方法 1
    public ReasonEngine(int maxIterCount, int sampling) {
        super(maxIterCount, sampling);
    }

    // 构造方法 2
    public ReasonEngine(Accumulator accumulator, int maxIterCount, int sampling) {
        super(accumulator, maxIterCount, sampling);
    }

    // ==============================
    // 完全匹配父类签名：无泛型冲突
    // ==============================
    @Override
    public List<MCTSNode<ReasoningState<?>>> onExpanded(@NonNull MCTSNode<ReasoningState<?>> node) {
        List<MCTSNode<ReasoningState<?>>> children = new ArrayList<>();
        ReasoningState<?> state = node.getState();

        // 终止状态不扩展
        if (isTerminalState(node)) {
            return children;
        }

        // if current node has been expanded, then return empty list.
        if(node.hasExpanded())
            return children;
        var action = node.nextAction(ThreadLocalRandom.current(), false);
        for(int i = 0; i < sampling; ++i) {
            var child = action.transit(node);
            children.add(child);
        }
        return children;
    }

    // ==============================
    // 模拟阶段（Rollout）
    // ==============================
    @Override
    public double onSimulate(@NonNull MCTSNode<ReasoningState<?>> node) {
        MCTSNode<ReasoningState<?>> current = node;
        int steps = 0;

        while (steps < MAX_ROLLOUT_DEPTH && !isTerminalState(current)) {
            List<MCTSNode<ReasoningState<?>>> children = onExpanded(current);
            if (children.isEmpty()) break;

            current = children.get(random.nextInt(children.size()));
            steps++;
        }

        return getTerminalReward(current);
    }

    // ==============================
    // 终止状态判断
    // ==============================
    @Override
    public boolean isTerminalState(@NonNull MCTSNode<ReasoningState<?>> node) {
        var planStep = node.getState().getPlanStep();
        if(planStep == null) return true;
        var nextPlanStep = planStep.next();
        if(nextPlanStep == null) return true;
        return node.getState().isFinal()
                || node.getDepth() >= MAX_ROLLOUT_DEPTH;
    }

    // ==============================
    // 终止奖励函数
    // ==============================
    @Override
    public double getTerminalReward(@NonNull MCTSNode<ReasoningState<?>> node) {
        return node.getState().getConfidence();
    }
}