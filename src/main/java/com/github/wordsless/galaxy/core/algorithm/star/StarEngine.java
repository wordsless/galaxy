package com.github.wordsless.galaxy.core.algorithm.star;

import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSEngine;
import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSNode;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG-Star 官方实现引擎
 * 严格对应 arXiv:2412.12881 论文
 */
public class StarEngine extends MCTSEngine<StarState> {

    // 论文超参：每次扩展生成 m_q 个子节点
    private final int mq;

    // 策略模型 π_θ（LLM，生成子查询、子答案、最终答案）
    private final PolicyModel policyModel;

    // 奖励模型（打分 r_q, r_a → 最终 reward）
    private final RewardModel rewardModel;

    // 检索器（仅 Simulation 阶段使用）
    private final Retriever retriever;

    public StarEngine(int maxIterCount, int sampling, int mq,
                      PolicyModel policyModel,
                      RewardModel rewardModel,
                      Retriever retriever) {
        super(maxIterCount, sampling);
        this.mq = mq;
        this.policyModel = policyModel;
        this.rewardModel = rewardModel;
        this.retriever = retriever;
    }

    // ==========================================
    // 【核心 1：扩展节点】
    // 论文 4.3 Plan Expansion
    // 输入：当前节点
    // 输出：m_q 个子节点
    // ==========================================
    @Override
    public List<MCTSNode<StarState>> onExpanded(MCTSNode<StarState> node) {
        List<MCTSNode<StarState>> children = new ArrayList<>();
        StarState currState = node.getState();

        // 论文：π_θ 根据历史生成 m_q 组 (子查询q, 子答案a)
        List<PolicyModel.Step> steps = policyModel.generateSteps(
                currState.getOriginalQuestion(),
                currState.getHistory(),
                mq
        );

        // 每一步生成一个子节点
        for (PolicyModel.Step step : steps) {
            String newHistory = currState.getHistory() + "\n" +
                    "Q: " + step.getSubQuery() + "\n" +
                    "A: " + step.getSubAnswer();

            // 构建新状态
            StarState newState = new StarState(
                    currState.getOriginalQuestion(),
                    step.getSubQuery(),
                    step.getSubAnswer(),
                    newHistory,
                    step.isFinal()
            );

            // 构建子节点
            MCTSNode<StarState> child = new MCTSNode<>(node, newState);
            child.setDepth(node.getDepth() + 1);
            children.add(child);
        }

        return children;
    }

    // ==========================================
    // 【核心 2：模拟阶段 → 计算奖励】
    // 论文 4.4 Retrieval-Augmented Reward
    // ==========================================
    @Override
    public double onSimulate(MCTSNode<StarState> node) {
        StarState state = node.getState();

        // 终止状态直接返回最终奖励
        if (isTerminalState(node)) {
            return getTerminalReward(node);
        }

        // ============= RAG-Star 奖励计算 =============
        // 1. 用子查询检索（仅在 Simulation 检索）
        String query = state.getSubQuery();
        String docs = retriever.retrieve(query);

        // 2. 计算奖励 r = r_q × r_a
        double rq = rewardModel.computeQueryReward(state.getHistory(), query);
        double ra = rewardModel.computeAnswerReward(docs, state.getSubAnswer());
        double reward = rq * ra;

        return reward;
    }

    // ==========================================
    // 终止状态判断（论文：叶子节点=推理结束）
    // ==========================================
    @Override
    public boolean isTerminalState(MCTSNode<StarState> node) {
        // 1. LLM 标记这是最后一步
        // 2. 没有子节点
        return node.getState().isFinal() || node.getChildren().isEmpty();
    }

    // ==========================================
    // 终止奖励（最终步骤给满分）
    // ==========================================
    @Override
    public double getTerminalReward(MCTSNode<StarState> node) {
        return 3.0; // 论文最高分为3
    }

    // ==========================================
    // 【最终输出】从树中选出最优路径 → 生成答案
    // ==========================================
    public String getFinalAnswer(MCTSNode<StarState> root) {
        // 选 Q 值最高的路径
        MCTSNode<StarState> bestNode = selectBestNode(root);
        StarState bestState = bestNode.getState();

        // 论文：用 π_θ 总结生成最终答案
        return policyModel.generateFinalAnswer(
                bestState.getOriginalQuestion(),
                bestState.getHistory()
        );
    }
}