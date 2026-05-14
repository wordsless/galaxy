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

package com.github.wordsless.galaxy.core.orchestrator;

import com.github.wordsless.galaxy.core.algorithm.mcts.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class AutonomousStrategicPlanningOrchestrator extends AbstractBasicOrchestrator {

    private final ReasoningTreeNodeFactory factory;
    private final Simulator simulator;
    private final Accumulator accumulator;
    private final ConsistencyVoter voter;
    private final int sampling;
    private final int maxDepth;
    private final int minDepth;
    private final int maxIterCount;

    public AutonomousStrategicPlanningOrchestrator(
            final ReasoningTreeNodeFactory factory,
            final Simulator simulator,
            final Accumulator accumulator,
            final ConsistencyVoter voter,
            final int sampling,
            final int maxDepth,
            final int minDepth,
            final int maxIterCount) {
        this.factory = factory;
        this.simulator = simulator;
        this.accumulator = accumulator;
        this.voter = voter;
        this.sampling = sampling;
        this.maxDepth = maxDepth;
        this.minDepth = minDepth;
        this.maxIterCount = maxIterCount;
    }

    @Override
    public List<String> retrieve(Map<String, ?> context) {
        List<String> queries = (List<String>) context.get("RewritedMultiQueries");

        if (queries == null || queries.isEmpty()) {
            String rewrited = (String) context.get("RewritedQuery");
            queries = (rewrited != null && !rewrited.isEmpty()) ? List.of(rewrited) : null;
        }

        if (queries == null) {
            String rawQuery = (String) context.get("RawQuery");
            if (rawQuery == null || rawQuery.isEmpty()) {
                throw new IllegalStateException("RawQuery is null or empty");
            }
            queries = List.of(rawQuery);
        }

        var answers = new CopyOnWriteArrayList<String>();
        try(var executor = Executors.newFixedThreadPool(10)) {
            for(var query : queries) {
                executor.execute(() -> {
                    var engine = new ReasoningEngine(
                            this.factory,
                            this.simulator,
                            this.accumulator,
                            this.voter,
                            this.sampling,
                            this.maxDepth,
                            this.minDepth,
                            this.maxIterCount);
                    var answer = engine.getAnswer(query);
                    answers.add(answer);
                });
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return answers;
    }
}
