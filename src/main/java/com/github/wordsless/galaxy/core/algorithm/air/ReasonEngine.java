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