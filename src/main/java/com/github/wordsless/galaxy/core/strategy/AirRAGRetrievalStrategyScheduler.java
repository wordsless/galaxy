package com.github.wordsless.galaxy.core.strategy;

import com.github.wordsless.galaxy.core.*;
import com.github.wordsless.galaxy.core.algorithm.mcts.*;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Conversation;
import com.github.wordsless.galaxy.core.response.RetrievalQualityResponse;
import com.github.wordsless.galaxy.core.response.SummarizationAnswerResponse;
import com.github.wordsless.galaxy.core.response.DirectAnswerResponse;
import com.github.wordsless.galaxy.core.exception.RetrievalStrategyScheduleException;
import com.github.wordsless.galaxy.core.sample.ReasoningAction;
import lombok.Builder;
import lombok.NonNull;
import pub.rag.core.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AirRAG 检索策略调度器
 * 集成 MCTS 推理树搜索与自一致性验证，实现自主规划和推理导向的检索增强生成
 *
 * @param <T> 质量评估响应类型（通常为 RetrievalQualityResponse）
 */
public class AirRAGRetrievalStrategyScheduler<T extends RetrievalQualityResponse>
        extends AbstractRetrievalStrategyScheduler<T> {

    // ==================== MCTS 配置参数 ====================
    private final int maxIterations;
    private final int maxDepth;
    private final int simulationMinDepth;
    private final int simulationMaxDepth;
    private final double uctExplorationConstant;

    // ==================== 工厂与组件 ====================
    private final ReasonTreeNodeFactory nodeFactory;
    private final ReasoningTreeNodeSimulator simulator;
    private final SingleRetrievalStrategyScheduler<?> retrievalScheduler;
    private final Map<ReasoningAction, List<ReasoningAction>> actionGraph;

    // ==================== 请求模板 ====================
    private final ChatModelRequest systemAnalysisRequest;
    private final ChatModelRequest queryTransformRequest;
    private final ChatModelRequest retrievalAnswerRequest;
    private final ChatModelRequest directAnswerRequest;
    private final ChatModelRequest summarizationAnswerRequest;

    // ==================== 一致性验证配置 ====================
    private final ConsistencyVotingMethod votingMethod;
    private final boolean enableSelfConsistency;

    public enum ConsistencyVotingMethod {
        JACCARD,
        EMBEDDING,
        NONE
    }

    @Builder
    public AirRAGRetrievalStrategyScheduler(
            // 继承自父类的组件
            String template,
            ChatModelDelegator chatModelDelegator,
            Preprocessor preprocessor,
            Retriever retriever,
            Aligner aligner,
            Reranker reranker,

            // MCTS 参数
            int maxIterations,
            int maxDepth,
            int simulationMinDepth,
            int simulationMaxDepth,
            double uctExplorationConstant,

    // 核心组件
    @NonNull ReasonTreeNodeFactory nodeFactory,
    @NonNull ReasoningTreeNodeSimulator simulator,
    @NonNull SingleRetrievalStrategyScheduler<?> retrievalScheduler,
    @NonNull Map<ReasoningAction, List<ReasoningAction>> actionGraph,

    // 请求模板
    @NonNull ChatModelRequest systemAnalysisRequest,
    @NonNull ChatModelRequest queryTransformRequest,
    @NonNull ChatModelRequest retrievalAnswerRequest,
    @NonNull ChatModelRequest directAnswerRequest,
    @NonNull ChatModelRequest summarizationAnswerRequest,

    // 一致性验证
    ConsistencyVotingMethod votingMethod,
    boolean enableSelfConsistency
    ) {
        super(template, chatModelDelegator, preprocessor, retriever, aligner, reranker);
        this.maxIterations = maxIterations;
        this.maxDepth = maxDepth;
        this.simulationMinDepth = simulationMinDepth;
        this.simulationMaxDepth = simulationMaxDepth;
        this.uctExplorationConstant = uctExplorationConstant;
        this.nodeFactory = nodeFactory;
        this.simulator = simulator;
        this.retrievalScheduler = retrievalScheduler;
        this.actionGraph = actionGraph;
        this.systemAnalysisRequest = systemAnalysisRequest;
        this.queryTransformRequest = queryTransformRequest;
        this.retrievalAnswerRequest = retrievalAnswerRequest;
        this.directAnswerRequest = directAnswerRequest;
        this.summarizationAnswerRequest = summarizationAnswerRequest;
        this.votingMethod = votingMethod;
        this.enableSelfConsistency = enableSelfConsistency;
    }

    /**
     * 实现 RetrievalStrategyScheduler 接口的 answer 方法
     * 调用 retrieve 并返回答案文本
     */
    @Override
    public String answer(String rawQuery) {
        T response = retrieve(rawQuery);
        if (response == null || response.getAnswer() == null) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No answer generated for query: [%s]", rawQuery));
        }
        return response.getAnswer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T retrieve(String rawQuery) {
        // 1. 初始化根节点（初始动作为 SAY）
        List<ReasoningAction> rootCandidates = actionGraph.getOrDefault(ReasoningAction.SAY, Collections.emptyList());
        ReasonTreeNode<?> root = new ReasonTreeNode<>(ReasoningAction.SAY, rootCandidates, rawQuery);

        // 2. 构建 MCTS 遍历器
        MetricAccumulator accumulator = new UCTAccumulator();
        ReasoningTreeTraverser traverser = new ReasoningTreeTraverser(
                rawQuery, accumulator, nodeFactory, simulator, retrievalScheduler,
                maxDepth, maxIterations,
                systemAnalysisRequest, retrievalAnswerRequest, directAnswerRequest,
                queryTransformRequest, summarizationAnswerRequest
        );

        // 3. 运行 MCTS 迭代
        traverser.iterate(root);

        // 4. 收集所有终端节点中的候选答案
        List<ReasonTreeNode<?>> terminalNodes = collectTerminalNodes(root);
        List<String> candidateAnswers = extractAnswersFromNodes(terminalNodes);

        if (candidateAnswers.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No candidate answers generated for query: [%s]", rawQuery));
        }

        // 5. 自一致性验证：选择最佳答案
        String finalAnswer;
        double confidence;
        if (enableSelfConsistency && candidateAnswers.size() > 1) {
            BestAnswerResult result = selectBestAnswerByVoting(candidateAnswers);
            finalAnswer = result.answer;
            confidence = result.confidence;
        } else {
            finalAnswer = candidateAnswers.get(0);
            confidence = 0.5;
        }

        // 6. 构造返回对象（假设 RetrievalQualityResponse 有合适的构造函数）
        T response = (T) new RetrievalQualityResponse(finalAnswer, confidence, Collections.emptyList());
        return response;
    }

    /**
     * 收集所有终端节点（没有子节点，且动作为 SA 或达到最大深度）
     */
    private List<ReasonTreeNode<?>> collectTerminalNodes(ReasonTreeNode<?> root) {
        List<ReasonTreeNode<?>> terminals = new ArrayList<>();
        collectTerminalsRecursive(root, terminals);
        return terminals;
    }

    private void collectTerminalsRecursive(ReasonTreeNode<?> node, List<ReasonTreeNode<?>> collector) {
        if (node.isLeaf()) {
            if (node.getAction() == ReasoningAction.SA || node.getDepth() >= maxDepth) {
                collector.add(node);
            }
            return;
        }
        for (var child : node.getChildren()) {
            collectTerminalsRecursive((ReasonTreeNode<?>) child, collector);
        }
    }

    /**
     * 从节点中提取答案文本
     */
    private List<String> extractAnswersFromNodes(List<ReasonTreeNode<?>> nodes) {
        List<String> answers = new ArrayList<>();
        for (ReasonTreeNode<?> node : nodes) {
            String ans = null;
            Object data = node.getData();
            if (data instanceof SummarizationAnswerResponse) {
                ans = ((SummarizationAnswerResponse) data).getFinalAnswer();
            } else if (data instanceof DirectAnswerResponse) {
                ans = ((DirectAnswerResponse) data).getAnswer();
            } else if (data instanceof RetrievalQualityResponse) {
                ans = ((RetrievalQualityResponse) data).getAnswer();
            }
            if (ans != null && !ans.trim().isEmpty()) {
                answers.add(ans.trim());
            } else {
                // 兜底：从 state 最后一条 assistant/tool 消息中提取
                List<Conversation> state = node.getState();
                for (int i = state.size() - 1; i >= 0; i--) {
                    Conversation msg = state.get(i);
                    if ("assistant".equals(msg.getRole()) || "tool".equals(msg.getRole())) {
                        String content = msg.getContent();
                        if (content != null && !content.trim().isEmpty()) {
                            ans = content.replaceAll("^\\[[A-Z]{2}]\\s*", "");
                            break;
                        }
                    }
                }
                if (ans != null && !ans.trim().isEmpty()) {
                    answers.add(ans.trim());
                }
            }
        }
        return answers;
    }

    /**
     * 自一致性投票选择最佳答案
     */
    private BestAnswerResult selectBestAnswerByVoting(List<String> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidate list cannot be empty");
        }
        if (candidates.size() == 1) {
            return new BestAnswerResult(candidates.get(0), 1.0);
        }

        switch (votingMethod) {
            case JACCARD:
                return jaccardVoting(candidates);
            case EMBEDDING:
                // 嵌入投票需要 embedding 服务，此处回退到 Jaccard
                return jaccardVoting(candidates);
            case NONE:
            default:
                return new BestAnswerResult(candidates.get(0), 0.5);
        }
    }

    /**
     * 基于 Jaccard 相似度的投票算法
     */
    private BestAnswerResult jaccardVoting(List<String> candidates) {
        int n = candidates.size();
        double[] scores = new double[n];

        List<Set<String>> tokenSets = candidates.stream()
                .map(this::tokenizeToSet)
                .collect(Collectors.toList());

        for (int i = 0; i < n; i++) {
            Set<String> setI = tokenSets.get(i);
            double totalSimilarity = 0.0;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                Set<String> setJ = tokenSets.get(j);
                double intersection = 0.0;
                for (String token : setI) {
                    if (setJ.contains(token)) intersection++;
                }
                double union = setI.size() + setJ.size() - intersection;
                double jaccard = (union == 0) ? 0 : intersection / union;
                totalSimilarity += jaccard;
            }
            scores[i] = totalSimilarity / (n - 1);
        }

        int bestIdx = 0;
        for (int i = 1; i < n; i++) {
            if (scores[i] > scores[bestIdx]) bestIdx = i;
        }
        return new BestAnswerResult(candidates.get(bestIdx), scores[bestIdx]);
    }

    /**
     * 简单分词：按空白符、标点分割，转为小写
     */
    private Set<String> tokenizeToSet(String text) {
        if (text == null) return Collections.emptySet();
        String[] words = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ")
                .split("\\s+");
        return new HashSet<>(Arrays.asList(words));
    }

    private static class BestAnswerResult {
        final String answer;
        final double confidence;
        BestAnswerResult(String answer, double confidence) {
            this.answer = answer;
            this.confidence = confidence;
        }
    }

    @Override
    public void close() throws Exception {
        if (retrievalScheduler != null) {
            retrievalScheduler.close();
        }
    }
}