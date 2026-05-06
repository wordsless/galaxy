package com.github.wordsless.galaxy.core.algorithm.mcts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.response.SimulationResponse;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ReasoningTreeNodeSimulator implements Simulator {

    private final Map<ReasoningAction, List<ReasoningAction>> candidates;
    private final ChatModelRequest<SimulationResponse> requestTemplate;
    private final ChatModelDelegator<SimulationResponse> delegator;

    public ReasoningTreeNodeSimulator(ChatModelDelegator<SimulationResponse> delegator,
                                      ChatModelRequest<SimulationResponse> requestTemplate,
                                      Map<ReasoningAction, List<ReasoningAction>> candidates) {
        this.delegator = delegator;
        this.requestTemplate = requestTemplate;
        this.candidates = candidates;
    }

    @Override
    public double simulate(ReasonTreeNode<?> startNode, int minDepth, int maxDepth, List<String> history) {
        int depth = ThreadLocalRandom.current().nextInt(minDepth, maxDepth + 1);
        List<Map<ReasoningAction, Double>> path = new ArrayList<>();
        ReasoningAction currentAction = startNode.getAction();

        Map<ReasoningAction, Double> init = new HashMap<>(1);
        init.put(currentAction, 0.0);
        path.add(init);

        for (int i = 0; i < depth; i++) {
            if (currentAction == ReasoningAction.SA) break;
            List<ReasoningAction> legal = candidates.get(currentAction);
            if (legal == null || legal.isEmpty()) break;
            ReasoningAction nextAction = legal.get(ThreadLocalRandom.current().nextInt(legal.size()));
            double confidence = ThreadLocalRandom.current().nextDouble();
            Map<ReasoningAction, Double> step = new HashMap<>(1);
            step.put(nextAction, confidence);
            path.add(step);
            currentAction = nextAction;
            if (nextAction == ReasoningAction.SA) break;
        }

        ChatModelRequest<SimulationResponse> req = requestTemplate.toBuilder()
                .simulationSequence(path)
                .context(history)
                .build();
        SimulationResponse resp = delegator.delegate(req, new TypeReference<>() {});
        return resp.getReward();
    }
}