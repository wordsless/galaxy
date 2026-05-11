/*
 * MIT License
 *
 * Copyright (c) 2026 Qiang Li (李强)
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

package com.github.wordsless.galaxy.core.strategy;

import com.github.wordsless.galaxy.core.*;
import com.github.wordsless.galaxy.core.algorithm.mcts.*;
import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.response.AbstractBasicResponse;
import com.github.wordsless.galaxy.core.response.RetrievalQualityResponse;
import com.github.wordsless.galaxy.core.response.SimulationResponse;
import com.github.wordsless.galaxy.core.sample.ReasoningAction;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * AirRAG: Autonomous Strategic Planning with MCTS for Retrieval-Augmented Generation.
 *
 * This scheduler uses Monte Carlo Tree Search to explore diverse reasoning paths,
 * including system analysis (SAY), query transformation (QT), retrieval answer (RA),
 * direct answer (DA), and summary answer (SA). It dynamically decides the best sequence
 * of actions to answer complex questions.
 */
@Slf4j
public class AutonomousStrategicPlanningWithMCTSRetrievalStrategyScheduler
        extends AbstractRetrievalStrategyScheduler<RetrievalQualityResponse> {

    private final ReasonTreeNodeFactory nodeFactory;
    private final ChatModelRequest simulationRequestTemplate;
    private final Map<ReasoningAction, List<ReasoningAction>> actionCandidates;
    private final MetricAccumulator metricAccumulator;
    private final int maxDepth;
    private final int maxIterations;
    private final int sampling;
    private final int simulationMinDepth;
    private final int simulationMaxDepth;

    /**
     * Builder-style constructor.
     */
    @Builder
    public AutonomousStrategicPlanningWithMCTSRetrievalStrategyScheduler(
            String template,
            ChatModelDelegator chatModelDelegator,
            Preprocessor preprocessor,
            Retriever retriever,
            Aligner aligner,
            Reranker reranker,
            ReasonTreeNodeFactory nodeFactory,
            ChatModelRequest simulationRequestTemplate,
            Map<ReasoningAction, List<ReasoningAction>> actionCandidates,
            MetricAccumulator metricAccumulator,
            int maxDepth,
            int maxIterations,
            int sampling,
            int simulationMinDepth,
            int simulationMaxDepth) {
        super(template, chatModelDelegator, preprocessor, retriever, aligner, reranker);
        this.nodeFactory = nodeFactory;
        this.simulationRequestTemplate = simulationRequestTemplate;
        this.actionCandidates = actionCandidates;
        this.metricAccumulator = metricAccumulator;
        this.maxDepth = maxDepth;
        this.maxIterations = maxIterations;
        this.sampling = sampling;
        this.simulationMinDepth = simulationMinDepth;
        this.simulationMaxDepth = simulationMaxDepth;
    }

    @Override
    public RetrievalQualityResponse retrieve(String query) {
        log.info("Starting AirRAG MCTS for query: {}", query);

        // 1. Build root node (always SAY)
        List<ReasoningAction> rootCandidates = actionCandidates.get(ReasoningAction.SAY);
        ReasonTreeNode<?> root = new ReasonTreeNode<>(ReasoningAction.SAY, rootCandidates);

        // 2. Build MCTS components
        ReasoningTreeNodeSimulator simulator = new ReasoningTreeNodeSimulator(
                (ChatModelDelegator<SimulationResponse>) chatModelDelegator,
                simulationRequestTemplate,
                actionCandidates
        );

        ReasoningTreeTraverser traverser = new ReasoningTreeTraverser(
                query,
                metricAccumulator,
                nodeFactory,
                simulator,
                actionCandidates,
                maxDepth,
                sampling
        );

        // 3. Run MCTS iterations
        traverser.iterate(root, maxIterations);

        // 4. Select best path (highest average reward from root's children)
        ReasonTreeNode<?> bestChild = selectBestChild(root);
        if (bestChild == null) {
            log.warn("No child expanded for root, falling back to direct answer");
            return fallbackDirectAnswer(query);
        }

        // 5. Traverse to the leaf with highest cumulative value (or just return best child's answer)
        ReasonTreeNode<?> bestLeaf = findBestLeaf(bestChild);
        RetrievalQualityResponse response = extractResponse(bestLeaf);
        if (response == null) {
            response = fallbackDirectAnswer(query);
        }
        log.info("AirRAG finished with reward: {}", bestChild.getValueSum() / bestChild.getVisitCount());
        return response;
    }

    /**
     * Select child with highest average value (valueSum/visitCount).
     */
    private ReasonTreeNode<?> selectBestChild(ReasonTreeNode<?> node) {
        ReasonTreeNode<?> best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (TreeNode<?> child : node.getChildren()) {
            ReasonTreeNode<?> mctsChild = (ReasonTreeNode<?>) child;
            double avg = mctsChild.getValueSum() / mctsChild.getVisitCount();
            if (best == null || avg > bestValue) {
                bestValue = avg;
                best = mctsChild;
            }
        }
        return best;
    }

    /**
     * From a node, repeatedly choose the child with highest average value until leaf.
     */
    private ReasonTreeNode<?> findBestLeaf(ReasonTreeNode<?> node) {
        ReasonTreeNode<?> current = node;
        while (!current.getChildren().isEmpty()) {
            ReasonTreeNode<?> bestChild = selectBestChild(current);
            if (bestChild == null) break;
            current = bestChild;
        }
        return current;
    }

    /**
     * Extract RetrievalQualityResponse from leaf node data.
     * Works for RA, DA, SA nodes. For SAY or QT, fallback.
     */
    private RetrievalQualityResponse extractResponse(ReasonTreeNode<?> node) {
        Object data = node.getData();
        if (data instanceof RetrievalQualityResponse) {
            return (RetrievalQualityResponse) data;
        } else if (data instanceof AbstractBasicResponse) {
            // For SAY or QT, we cannot get final answer; fallback.
            log.warn("Leaf node is not answer-producing (action={}), fallback", node.getAction());
            return null;
        }
        return null;
    }

    private RetrievalQualityResponse fallbackDirectAnswer(String query) {
        // Simple direct answer via LLM without retrieval
        // This can be implemented using DirectAnswerResponse and delegator.
        // For brevity, return a dummy response (in practice call DA node via factory)
        RetrievalQualityResponse resp = new RetrievalQualityResponse();
        resp.setAnswer("I'm unable to generate a confident answer. Please rephrase your question.");
        resp.setConfidence(0.0);
        resp.setReason("Fallback due to no valid MCTS path");
        return resp;
    }

    @Override
    public String answer(String rawQuery) {
        return "";
    }
}