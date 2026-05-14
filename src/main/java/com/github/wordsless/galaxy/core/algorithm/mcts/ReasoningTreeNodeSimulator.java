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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.ChatModelRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 实现带 rollout 的模拟器。
 * 在模拟阶段，从 startNode 开始，随机选择合法的后续动作（满足依赖且未使用），
 * 调用 LLM 生成子节点，继续下一轮，直到达到 maxDepth 或无合法动作。
 * 最后对终止节点的答案进行质量评估，累积奖励（无折扣，即每步奖励相加）。
 */
public class ReasoningTreeNodeSimulator implements Simulator {
    private static final Logger log = LoggerFactory.getLogger(ReasoningTreeNodeSimulator.class);

    private final ChatModelDelegator<List<Double>> qualityEvaluator;
    private final ChatModelRequest requestTemplate;
    private final ReasoningTreeNodeFactory factory;
    private final Random random = new Random();

    /**
     * 构造模拟器。
     * @param qualityEvaluator 质量评估器（LLM 调用），返回 [0,1] 分数
     * @param requestTemplate  请求模板，用于构建质量评估请求
     * @param factory          节点工厂，用于在 rollout 中创建临时子节点
     */
    public ReasoningTreeNodeSimulator(ChatModelDelegator<List<Double>> qualityEvaluator,
                                      ChatModelRequest requestTemplate,
                                      ReasoningTreeNodeFactory factory) {
        this.qualityEvaluator = qualityEvaluator;
        this.requestTemplate = requestTemplate;
        this.factory = factory;
    }

    @Override
    public double simulate(ReasoningTreeNode<?, ?> startNode, int minDepth, int maxDepth) {
        if (startNode == null) return 0.0;

        ReasoningTreeNode<?, ?> current = startNode;
        int depth = current.getDepth();

        // 累积奖励（通常为所有中间步奖励之和，这里简单累积）
        double totalReward = 0.0;

        while (depth < maxDepth) {
            // 获取当前节点的所有合法候选动作（依赖已满足且未使用）
            List<PlanStep> availableActions = getAvailableActions(current);
            if (availableActions.isEmpty()) {
                break; // 无动作可执行，终止 rollout
            }

            // 随机选择一个动作
            PlanStep chosen = availableActions.get(random.nextInt(availableActions.size()));

            // 创建执行该动作后的临时子节点（注意：此处创建的子节点不会记录到主搜索树中）
            ReasoningTreeNode<?, ?> nextNode = factory.createNode(chosen, current);

            if (nextNode == null) {
                log.warn("Failed to create node for action: {}", chosen.getAction());
                break;
            }

            // 评估这一步骤的即时奖励（可选：使用质量评估器或简单固定值）
            double stepReward = evaluateStepReward(nextNode);
            totalReward += stepReward;

            // 继续下一步
            current = nextNode;
            depth = current.getDepth();
        }

        // 最终节点的质量评估（也可以使用已有的 qualityEvaluator）
        double finalReward = evaluateFinalNode(current);
        totalReward += finalReward;

        // 归一化到 [0,1] 区间（可根据需要调整）
        return Math.min(1.0, Math.max(0.0, totalReward / (maxDepth + 1)));
    }

    /**
     * 获取当前节点所有可执行的候选动作。
     * 条件：动作尚未使用（used == false），且其所有依赖（dependOn）已经出现在祖先节点的执行序列中。
     */
    private List<PlanStep> getAvailableActions(ReasoningTreeNode<?, ?> node) {
        PlanStep plan = node.getPlanStep();
        if (plan == null || plan.getChildren().isEmpty()) {
            return List.of();
        }

        // 收集从根到当前节点的已执行动作的 SN 集合
        java.util.Set<Integer> executedSNs = collectExecutedSNs(node);

        return plan.getChildren().stream()
                .filter(candidate -> !candidate.isUsed())
                .filter(candidate -> dependenciesSatisfied(candidate, executedSNs))
                .collect(Collectors.toList());
    }

    /**
     * 收集从根到当前节点路径上所有已经执行过的动作 SN。
     * 简单实现：遍历父链，收集每个节点的 planStep.SN（如果存在且已执行）。
     */
    private java.util.Set<Integer> collectExecutedSNs(ReasoningTreeNode<?, ?> node) {
        java.util.Set<Integer> executed = new java.util.HashSet<>();
        ReasoningTreeNode<?, ?> cur = node;
        while (cur != null) {
            PlanStep step = cur.getPlanStep();
            if (step != null && step.getSN() != null && step.getSN() > 0) {
                // 假设节点对应的计划步骤已经执行（节点存在即代表该动作已执行）
                executed.add(step.getSN());
            }
            cur = cur.getParent();
        }
        return executed;
    }

    private boolean dependenciesSatisfied(PlanStep candidate, java.util.Set<Integer> executedSNs) {
        Integer dependOn = candidate.getDependOn();
        if (dependOn == null) return true;
        return executedSNs.contains(dependOn);
    }

    private double evaluateStepReward(ReasoningTreeNode<?, ?> node) {
        // 简单实现：如果节点没有答案，返回 0；否则调用质量评估器（轻量级模拟）
        String answer = extractAnswer(node);
        if (answer == null || answer.isEmpty()) return 0.0;
        // 避免重复调用质量评估器开销过大，可返回固定小奖励（例如 0.1）或基于规则
        return 0.1; // 占位符，实际应使用轻量评估
    }

    private double evaluateFinalNode(ReasoningTreeNode<?, ?> node) {
        String answer = extractAnswer(node);
        if (answer == null || answer.isEmpty()) return 0.0;
        ChatModelRequest request = requestTemplate.toBuilder()
                .rawQuery(extractOriginalQuery(node))
                .build();
        request.getContext().addAll(node.getContext());
        var reward = qualityEvaluator.delegate(request, new TypeReference<List<Double>>() {});
        return reward == null || reward.isEmpty() ? 0.0 : Math.max(0.0, Math.min(1.0, reward.get(0)));
    }

    private String extractAnswer(ReasoningTreeNode<?, ?> node) {
        Object data = node.getAnswer();
        if (data instanceof String) return (String) data;
        // fallback: 从上下文中提取最后一条 assistant 消息
        var context = node.getContext();
        for (int i = context.size() - 1; i >= 0; i--) {
            var msg = context.get(i);
            if ("assistant".equals(msg.getRole()) || "tool".equals(msg.getRole())) {
                String content = msg.getContent();
                if (content != null && content.contains("] ")) {
                    return content.substring(content.indexOf("] ") + 2);
                }
                return content;
            }
        }
        return null;
    }

    private String extractOriginalQuery(ReasoningTreeNode<?, ?> node) {
        ReasoningTreeNode<?, ?> cur = node;
        while (cur.getParent() != null) cur = cur.getParent();
        Object data = cur.getAnswer();
        return data instanceof String ? (String) data : "";
    }
}