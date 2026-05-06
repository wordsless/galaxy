package com.github.wordsless.galaxy.core.algorithm.mcts;

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import com.github.wordsless.galaxy.core.response.ConfidenceResponse;

import java.util.*;

public class ReasoningTreeTraverser {

    private final String rawQuery;
    private final MetricAccumulator metricAccumulator;
    private final ReasonTreeNodeFactory factory;
    private final ReasoningTreeNodeSimulator simulator;
    private final Map<ReasoningAction, List<ReasoningAction>> actionCandidates;
    private final int maxDepth;
    private final int sampling;

    public ReasoningTreeTraverser(String rawQuery,
                                  MetricAccumulator metricAccumulator,
                                  ReasonTreeNodeFactory factory,
                                  ReasoningTreeNodeSimulator simulator,
                                  Map<ReasoningAction, List<ReasoningAction>> actionCandidates,
                                  int maxDepth,
                                  int sampling) {
        this.rawQuery = rawQuery;
        this.metricAccumulator = metricAccumulator;
        this.factory = factory;
        this.simulator = simulator;
        this.actionCandidates = actionCandidates;
        this.maxDepth = maxDepth;
        this.sampling = sampling;
    }

    public void iterate(ReasonTreeNode<?> root, int maxIterations) {
        for (int iter = 0; iter < maxIterations; iter++) {
            // 1. Selection: get path from root to node to expand
            List<ReasonTreeNode<?>> path = select(root);
            if (path == null || path.isEmpty()) continue;
            ReasonTreeNode<?> selected = path.get(path.size() - 1);

            // Build history from the path (excluding the selected node itself)
            List<String> history = buildHistory(path, false);

            // 2. Expansion: create child nodes with diverse sampling
            List<ReasonTreeNode<?>> children = expand(selected, history);
            if (children.isEmpty()) continue;

            // 3. Simulation: for each child, simulate with full path history
            Map<ReasonTreeNode<?>, Double> rewards = new HashMap<>();
            for (ReasonTreeNode<?> child : children) {
                List<ReasonTreeNode<?>> fullPath = new ArrayList<>(path);
                fullPath.add(child);
                List<String> simHistory = buildHistory(fullPath, true);
                double reward = simulator.simulate(child, 1, 8, simHistory);
                rewards.put(child, reward);
            }

            // 4. Backpropagation
            for (Map.Entry<ReasonTreeNode<?>, Double> entry : rewards.entrySet()) {
                backpropagate(entry.getKey(), entry.getValue());
            }
        }
    }

    private List<ReasonTreeNode<?>> select(ReasonTreeNode<?> root) {
        List<ReasonTreeNode<?>> path = new ArrayList<>();
        ReasonTreeNode<?> current = root;
        path.add(current);
        while (true) {
            if (current.getDepth() >= maxDepth) return path;
            if (!current.isFullyExpanded()) return path;
            if (current.getChildren().isEmpty()) return path;
            ReasonTreeNode<?> best = selectBestChild(current);
            if (best == null) return path;
            path.add(best);
            current = best;
        }
    }

    private ReasonTreeNode<?> selectBestChild(ReasonTreeNode<?> parent) {
        double bestUcb = Double.NEGATIVE_INFINITY;
        ReasonTreeNode<?> best = null;
        for (TreeNode<?> child : parent.getChildren()) {
            ReasonTreeNode<?> mctsChild = (ReasonTreeNode<?>) child;
            double ucb = metricAccumulator.accumulate(mctsChild);
            if (best == null || ucb > bestUcb) {
                bestUcb = ucb;
                best = mctsChild;
            }
        }
        return best;
    }

    private List<String> buildHistory(List<ReasonTreeNode<?>> path, boolean includeLast) {
        List<String> history = new ArrayList<>();
        int end = includeLast ? path.size() : path.size() - 1;
        for (int i = 0; i < end; i++) {
            ReasonTreeNode<?> node = path.get(i);
            String step = String.format("Step %d: %s", i, node.getAction());
            if (node.getData() != null) {
                ConfidenceResponse data = node.getData();
                step += String.format("(confidence: %.2f, answer: %s)", data.getConfidence(), data.getAnswer());
            }
            history.add(step);
        }
        return history;
    }

    private List<ReasonTreeNode<?>> expand(ReasonTreeNode<?> node, List<String> history) {
        List<ReasonTreeNode<?>> samples = new ArrayList<>();
        if (node.getDepth() >= maxDepth) return samples;
        ReasoningAction action = node.nextAction();
        if (action == null) return samples;

        for (int i = 0; i < sampling; i++) {
            ReasonTreeNode<?> child = createChildNode(node, action, history);
            if (child != null) {
                node.addChild(child);
                samples.add(child);
            }
        }
        return samples;
    }

    private ReasonTreeNode<?> createChildNode(ReasonTreeNode<?> parent, ReasoningAction action, List<String> history) {
        String subquery = rawQuery; // simplify; could be derived from parent data
        try {
            return switch (action) {
                case SAY -> factory.createSAY(parent, rawQuery, subquery, history);
                case QT -> factory.createQT(parent, rawQuery, subquery, history);
                case RA -> factory.createRA(parent, rawQuery, subquery, history);
                case DA -> factory.createDA(parent, rawQuery, subquery, history);
                case SA -> factory.createSA(parent, rawQuery, subquery, history);
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void backpropagate(ReasonTreeNode<?> node, double reward) {
        ReasonTreeNode<?> current = node;
        while (current != null) {
            current.setVisitCount(current.getVisitCount() + 1);
            current.setValueSum(current.getValueSum() + reward);
            current = (ReasonTreeNode<?>) current.getParent();
        }
    }
}
