/*
 * MIT License
 * Copyright (c) 2024 Qiang Li (李强)
 */

package pub.rag.core.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import pub.rag.core.*;
import pub.rag.core.algorithm.mcts.*;
import pub.rag.core.algorithm.tree.*;
import pub.rag.core.entity.*;
import pub.rag.core.exception.ChatModelInvokerException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AIR-RAG调度器：基于MCTS的内在推理式检索增强生成
 * 支持查询改写(QT)、检索回答(RA)、直接回答(DA)、总结回答(SA)等推理动作的自主规划
 */
@Slf4j
public class AirRAGScheduler<T extends AirRAGResult> extends AbstractRetrievalStrategyScheduler<T> {

    // MCTS迭代次数
    private final int mctsIterations;
    // 最大推理深度
    private final int maxDepth;
    // 质量阈值（高于此值认为答案可靠）
    private final double qualityThreshold;

    // 用于构建提示词的Provider
    private final PromptProvider promptProvider;

    // 评分器、剪枝器、模拟器
    private final NodeScorer<String> nodeScorer;
    private final NodePruner<String> nodePruner;
    private final Simulator simulator;

    // MCTS访问器
    private final TreeNodeVisitorWithMCTS<AirRAGResult> mctsVisitor;

    // 构造函数（使用Builder模式简化参数）
    private AirRAGScheduler(final String template,
                            final ChatModelDelegator chatModelDelegator,
                            final Preprocessor preprocessor,
                            final Retriever retriever,
                            final Aligner aligner,
                            final Reranker reranker,
                            final int maxIter,
                            final int maxDepth,
                            final double qualityThreshold,
                            final PromptProvider promptProvider) {
        super(template, chatModelDelegator, preprocessor, retriever, aligner, reranker);
        this.mctsIterations     = maxIter;
        this.maxDepth           = maxDepth;
        this.qualityThreshold   = qualityThreshold;
        this.promptProvider     = promptProvider;

        // 初始化评分器、剪枝器、模拟器
        this.nodeScorer = new UCTScorer();
        this.nodePruner = new AlphaBetaPruner<>(nodeScorer);
        // TODO: 没写完，参数不能为null
        this.simulator = new AirRAGSimulator(null);

        // 创建MCTS访问器
        TreeNodeFactory<String> factory = new AirRAGNodeFactory<>();
        this.mctsVisitor = new AirRAGMCTSVisitor(simulator, factory, nodeScorer, nodePruner);
    }

    @Override
    public T retrieve(String query) {
        log.info("开始AIR-RAG推理，原始查询: {}", query);

        // 1. 初始化MCTS根节点
        AirRAGState initState = new AirRAGState(query, ReasoningAction.SAY);
        mctsVisitor.initialize(initState, initState);

        // 2. 执行MCTS迭代
        for (int i = 0; i < mctsIterations; i++) {
            try {
                mctsVisitor.iterate();
                log.debug("MCTS迭代 {} 完成", i);
            } catch (Exception e) {
                log.error("MCTS迭代 {} 失败: {}", i, e.getMessage(), e);
            }
        }

        // 3. 获取最优动作路径
        MCTSNode<AirRAGState> bestNode = mctsVisitor.getBestAction();
        if (bestNode == null) {
            log.warn("未找到有效动作路径，降级为直接检索");
            return fallbackRetrieve(query);
        }

        // 4. 沿着最优路径执行动作，得到最终答案
        String finalAnswer = executePath(bestNode);
        double confidence = computeConfidence(bestNode);

        AirRAGResult result = new AirRAGResult(finalAnswer, confidence);
        log.info("AIR-RAG推理完成，置信度: {}", confidence);
        return result;
    }

    @Override
    public String answer(String rawQuery) {
        AirRAGResult result = retrieve(rawQuery);
        if (result.getConfidence() >= qualityThreshold) {
            return result.getFinalAnswer();
        } else {
            // 降级：使用普通检索 + 生成
            return fallbackAnswer(rawQuery);
        }
    }

    @Override
    public void close() throws Exception {
        // 可在此释放资源，如线程池等
        log.info("关闭AIR-RAG调度器");
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 执行最优路径上的所有动作，生成最终答案
     */
    private String executePath(MCTSNode<AirRAGState> bestNode) {
        // 从根节点到bestNode收集所有State
        List<AirRAGState> path = new ArrayList<>();
        MCTSNode<AirRAGState> cur = bestNode;
        while (cur != null) {
            path.add(0, cur.getData());
            cur = (MCTSNode<AirRAGState>) cur.getParent();
        }

        StringBuilder answerBuilder = new StringBuilder();
        Map<String, String> nerCache = new HashMap<>();

        for (AirRAGState state : path) {
            log.debug("执行动作: {}, 内容: {}", state.getAction(), state.getContent());
            switch (state.getAction()) {
                case QT -> { // 查询转换
                    List<String> rewritten = preprocessor.process(state.getContent(), nerCache);
                    state.getContext().addAll(rewritten);
                }
                case RA -> { // 检索回答
                    String queryToRetrieve = state.getContext().isEmpty() ? state.getContent()
                            : state.getContext().get(state.getContext().size() - 1);
                    List<Document> docs = retriever.retrieve(queryToRetrieve);
                    if (docs != null && !docs.isEmpty()) {
                        List<Document> aligned = aligner.align(docs);
                        List<Document> reranked = reranker.rerank(aligned);
                        String retrievedFragment = reranked.stream()
                                .map(Document::getFragment)
                                .collect(Collectors.joining("\n"));
                        answerBuilder.append(retrievedFragment).append("\n");
                    }
                }
                case DA -> { // 直接回答
                    String directAnswer = generateAnswer(state.getContent(), state.getContext(), nerCache);
                    answerBuilder.append(directAnswer);
                }
                case SA -> { // 总结回答
                    String summary = summarizeAnswer(state.getContent(), state.getContext());
                    answerBuilder.append(summary);
                }
                case SAY -> {
                    // 系统分析：记录日志
                    log.debug("系统分析: {}", state.getContent());
                }
            }
        }

        return answerBuilder.toString().trim();
    }

    /**
     * 使用LLM生成答案
     */
    private String generateAnswer(String query, List<String> context, Map<String, String> entities) {
        PromptContext ctx = PromptContext.builder()
                .rawQuery(query)
                .entities(entities)
                .fragments(context)
                .build();
        // 调用ChatModelDelegator获取字符串答案（使用简单字符串类型）
        try {
            // 假设我们有一个无schema的调用，直接返回字符串
            // 实际中可以定义一个简单的StringResponse类型
            return chatModelDelegator.delegate(2, false, ctx, new TypeReference<String>() {});
        } catch (ChatModelInvokerException e) {
            log.error("生成答案失败", e);
            return "无法生成答案。";
        }
    }

    /**
     * 总结答案
     */
    private String summarizeAnswer(String query, List<String> context) {
        // 简化实现：取上下文中最后一段作为总结
        if (context.isEmpty()) return query;
        return context.get(context.size() - 1);
    }

    /**
     * 计算节点置信度
     */
    private double computeConfidence(MCTSNode<AirRAGState> node) {
        if (node.getVisitCount() == 0) return 0.0;
        return node.getAverageValue();
    }

    /**
     * 降级检索（传统RAG）
     */
    private AirRAGResult fallbackRetrieve(String query) {
        Map<String, String> ner = new HashMap<>();
        List<String> rewritten = preprocessor.process(query, ner);
        List<Document> allDocs = new ArrayList<>();
        for (String rq : rewritten) {
            allDocs.addAll(retriever.retrieve(rq));
        }
        List<Document> aligned = aligner.align(allDocs);
        List<Document> reranked = reranker.rerank(aligned);
        String answer = reranked.stream()
                .map(Document::getFragment)
                .collect(Collectors.joining("\n"));
        return new AirRAGResult(answer, 0.5);
    }

    private String fallbackAnswer(String query) {
        return fallbackRetrieve(query).getFinalAnswer();
    }


    /**
     * AIR-RAG MCTS访问器扩展：实现子节点数据生成
     */
    private class AirRAGMCTSVisitor extends TreeNodeVisitorWithMCTS<AirRAGState> {

        public AirRAGMCTSVisitor(Simulator simulator, TreeNodeFactory<String> factory,
                                 NodeScorer<String> scorer, NodePruner<String> pruner) {
            super(simulator, factory, scorer, pruner);
        }

        @Override
        protected AirRAGState generateChildData(MCTSNode<AirRAGState> parent) {
            AirRAGState parentState = parent.getData();
            ReasoningAction parentAction = parentState.getAction();

            // 根据父动作决定下一步可能的动作集合
            List<ReasoningAction> possibleActions = getPossibleNextActions(parentAction, parent.getDepth());
            // 随机选择一个动作（实际可使用LLM策略）
            ReasoningAction nextAction = possibleActions.get(new Random().nextInt(possibleActions.size()));

            // 生成子节点的内容（基于父节点上下文）
            String newContent = generateContentForAction(parentState, nextAction);
            return new AirRAGState(parentState, newContent, nextAction);
        }

        @Override
        protected Object generateChildState(MCTSNode<AirRAGState> parent) {
            // 子状态沿用父状态的状态对象（这里简化，直接返回父state）
            return parent.getState();
        }

        private List<ReasoningAction> getPossibleNextActions(ReasoningAction current, int depth) {
            List<ReasoningAction> actions = new ArrayList<>();
            if (depth >= maxDepth) {
                // 达到最大深度，只能总结
                actions.add(ReasoningAction.SA);
                return actions;
            }
            switch (current) {
                case SAY -> {
                    actions.add(ReasoningAction.QT);
                    actions.add(ReasoningAction.DA);
                }
                case QT -> {
                    actions.add(ReasoningAction.RA);
                    actions.add(ReasoningAction.DA);
                }
                case RA -> {
                    actions.add(ReasoningAction.SA);
                    actions.add(ReasoningAction.QT); // 可以多跳
                }
                case DA -> {
                    actions.add(ReasoningAction.SA);
                }
                default -> actions.add(ReasoningAction.SA);
            }
            return actions;
        }

        private String generateContentForAction(AirRAGState parentState, ReasoningAction action) {
            // 根据动作类型生成合理的文本内容
            // 简化实现：可使用LLM，这里用规则
            String parentContent = parentState.getContent();
            switch (action) {
                case QT -> {
                    // 查询改写：添加"改写:"前缀示例
                    return "改写: " + parentContent;
                }
                case RA -> {
                    return "检索: " + parentContent;
                }
                case DA -> {
                    return "直接回答: " + parentContent;
                }
                case SA -> {
                    return "总结: " + parentContent;
                }
                default -> {
                    return parentContent;
                }
            }
        }
    }
}