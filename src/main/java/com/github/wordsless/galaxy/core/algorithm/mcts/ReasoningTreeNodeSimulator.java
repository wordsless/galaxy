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
import com.github.wordsless.galaxy.core.entity.PromptContext;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ReasoningTreeNodeSimulator implements Simulator {

    private int maxDepth, minDepth;

    private Map<ReasoningAction, List<ReasoningAction>> candidates;

    private ChatModelDelegator delegator;

    static class PlanSimulationStep {
        String plan;
        ReasoningAction action;
        double confidence;
    }

    public ReasoningTreeNodeSimulator(final int maxDepth, final int minDepth, final ChatModelDelegator delegator) {
        this.maxDepth = maxDepth;
        this.minDepth = minDepth;
        candidates = new HashMap<>();
        candidates.put(ReasoningAction.SAY, Arrays.asList(new ReasoningAction[]{ReasoningAction.QT, ReasoningAction.RA, ReasoningAction.DA}));
        candidates.put(ReasoningAction.RA, Arrays.asList(new ReasoningAction[]{ReasoningAction.QT, ReasoningAction.RA, ReasoningAction.DA, ReasoningAction.SA}));
        candidates.put(ReasoningAction.DA, Arrays.asList(new ReasoningAction[]{ReasoningAction.SA}));
        candidates.put(ReasoningAction.QT, Arrays.asList(new ReasoningAction[]{ReasoningAction.RA, ReasoningAction.DA}));
        candidates.put(ReasoningAction.SA, null);
        this.delegator = delegator;
    }

    public ReasoningAction simulateExpansion(ReasoningAction parent) {
        var list = candidates.get(parent);
        var index = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }

    @Override
    public double simulate(ReasonTreeNode<?> node) {
        Random random = new Random();
        var d = random.nextInt(maxDepth - minDepth + 1) + minDepth;
        var sa = random.nextBoolean();

        return 0;
    }

    /**
     * SAY node simulation
     * @param plans SAY node plan list
     * @param context history
     * @return
     */
    private double simulateWithSAY(final String rawQuery, final List<String> plans, final List<String> context) {
        var random = new Random();
        var answers = new ArrayList<PlanSimulationStep>();
        for(var plan : plans) {
            var step = new PlanSimulationStep();
            step.plan = plan;
            step.confidence = random.nextDouble();
            step.action = simulateExpansion(ReasoningAction.SAY);
            answers.add(step);
        }
        // whether to add SA node
        if(random.nextBoolean()) {
            var step = new PlanSimulationStep();
            step.plan = "SUMMARY";
            step.confidence = random.nextDouble();
            step.action = ReasoningAction.SA;
            answers.add(step);
        }
        var promptContext = PromptContext.builder()
                                         .rawQuery(rawQuery)
                                         .history(context)
                                         .extra(answers)
                                         .build();
        return delegator.delegate(3, true, promptContext, new TypeReference<Double>() {});
    }

    private double simulateWithQT(final String rawQuery, final String subquery, final List<String> context) {

    }
}
