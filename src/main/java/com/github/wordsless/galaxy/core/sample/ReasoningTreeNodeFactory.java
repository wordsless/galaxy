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

package com.github.wordsless.galaxy.core.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Conversation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReasoningTreeNodeFactory {

    private final ChatModelDelegator<List<PlanStep>> systemAnalysisDelegator;

    private final ChatModelDelegator<List<String>> queryTransformDelegator;

    private final ChatModelDelegator<List<String>> retrievalQualityDelegator;

    private final ChatModelDelegator<List<String>>  directAnswerDelegator;

    private final ChatModelDelegator<List<String>> summarizationAnswerDelegator;

    private final ChatModelRequest systemAnalysisRequestTemplate;

    private final ChatModelRequest queryTransformRequestTemplate;

    private final ChatModelRequest retrievalAnswerRequestTemplate;

    private final ChatModelRequest directAnswerRequestTemplate;

    private final ChatModelRequest summarizationRequestTemplate;

    private final ObjectMapper objectMapper;

    private final ThreadLocalRandom random;

    @Autowired
    public ReasoningTreeNodeFactory(final ChatModelDelegator<List<PlanStep>> systemAnalysisDelegator,
                                    @Qualifier("queryTransformChatModelDelegator")
                                    final ChatModelDelegator<List<String>> queryTransformDelegator,
                                    @Qualifier("retrievalAnswerResponseChatModelDelegator")
                                    final ChatModelDelegator<List<String>> retrievalQualityDelegator,
                                    @Qualifier("directAnswerResponseChatModelDelegator")
                                    final ChatModelDelegator<List<String>>  directAnswerDelegator,
                                    @Qualifier("summaryAnswerChatModelDelegator")
                                    final ChatModelDelegator<List<String>> summarizationAnswerDelegator,
                                    @Qualifier("expansionRequest4SAY")
                                    final ChatModelRequest systemAnalysisRequestTemplate,
                                    @Qualifier("expansionRequest4QT")
                                    final ChatModelRequest queryTransformRequestTemplate,
                                    @Qualifier("expansionRequest4RA")
                                    final ChatModelRequest retrievalAnswerRequestTemplate,
                                    @Qualifier("expansionRequest4DA")
                                    final ChatModelRequest directAnswerRequestTemplate,
                                    @Qualifier("expansionRequest4SA")
                                    final ChatModelRequest summarizationRequestTemplate,
                                    final ObjectMapper objectMapper) {

        this.systemAnalysisDelegator        = systemAnalysisDelegator;
        this.queryTransformDelegator        = queryTransformDelegator;
        this.retrievalQualityDelegator      = retrievalQualityDelegator;
        this.directAnswerDelegator          = directAnswerDelegator;
        this.summarizationAnswerDelegator   = summarizationAnswerDelegator;

        this.systemAnalysisRequestTemplate  = systemAnalysisRequestTemplate;
        this.queryTransformRequestTemplate  = queryTransformRequestTemplate;
        this.retrievalAnswerRequestTemplate = retrievalAnswerRequestTemplate;
        this.directAnswerRequestTemplate    = directAnswerRequestTemplate;
        this.summarizationRequestTemplate   = summarizationRequestTemplate;

        this.objectMapper = objectMapper;
        this.random = ThreadLocalRandom.current();
    }

    public ReasoningTreeNode<String, List<PlanStep>>
    createSAY(final String query,
              final ReasoningTreeNode<?, ?> parent) {
        var request = this.systemAnalysisRequestTemplate.withQuery(query)
                                                        .withTemperature(this.random.nextDouble(1))
                                                        .withContext(parent.getContext());
        var vertexes = this.systemAnalysisDelegator.delegate(request, new TypeReference<List<PlanStep>>() {});
        var node = new ReasoningTreeNode<String, List<PlanStep>>(parent, null);

        var root = new PlanStep(-1, ReasoningAction.NONE, null);
        Map<Integer, PlanStep> snMap = new HashMap<>();
        for (var v : vertexes) {
            snMap.put(v.getSN(), v);
        }

        for (var v : vertexes) {
            var dependOn = v.getDependOn();
            if (dependOn != null) {
                PlanStep depended = snMap.get(dependOn);
                if (depended != null) {
                    depended.addChild(v);
                }
            } else {
                root.addChild(v);
            }
        }

        node.setAnswer(vertexes);
        node.setPlanStep(root);
        var context = new ArrayList<>(parent.getContext());
        context.add(new Conversation("user", query));
        try {
            context.add(new Conversation("system", this.objectMapper.writeValueAsString(vertexes)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        node.setContext(context);
        return node;
    }

    public ReasoningTreeNode<String, String>
    createSA(final String query,
             final ReasoningTreeNode<?, ?> parent) {
        var request = this.summarizationRequestTemplate.withQuery(query)
                                                        .withTemperature(this.random.nextDouble(1))
                                                       .withContext(parent.getContext());
        var summary = this.summarizationAnswerDelegator.delegate(request, new TypeReference<List<String>>() {});
        var node = new ReasoningTreeNode<String, String>(parent, null);
        node.setAnswer(summary.getFirst());
        var context = new ArrayList<>(parent.getContext());
        context.add(new Conversation("user", "make a summary."));
        context.add(new Conversation("system", summary.getFirst()));
        node.setContext(context);
        return node;
    }

    public ReasoningTreeNode<String, String>
    createDA(final String query, final ReasoningTreeNode<?, ?> parent, final PlanStep planStep) {
        var request = this.directAnswerRequestTemplate.withQuery(query)
                                                      .withTemperature(this.random.nextDouble(1))
                                                      .withContext(parent.getContext());
        var answer = this.directAnswerDelegator.delegate(request, new TypeReference<List<String>>() {});
        var node = new ReasoningTreeNode<String, String>(parent, planStep);
        node.setAnswer(answer.getFirst());
        var context = new ArrayList<>(parent.getContext());
        context.add(new Conversation("user", String.format("# Task\n%s\n# Query\n%s\n", request.getTask(), query)));
        context.add(new Conversation("system", answer.getFirst()));
        node.setContext(context);
        return node;
    }

    public ReasoningTreeNode<String, List<String>>
    createQT(final String query,
             final ReasoningTreeNode<?, ?> parent,
             final PlanStep planStep) {
        var request = this.queryTransformRequestTemplate.withQuery(query)
                                                        .withTemperature(this.random.nextDouble(1))
                                                        .withContext(parent.getContext());
        var transformed = this.queryTransformDelegator.delegate(request, new TypeReference<List<String>>() {});
        var node = new ReasoningTreeNode<String, List<String>>(parent, planStep);
        node.setAnswer(transformed);
        var sb = new StringBuilder();
        for(var t : transformed) {
            sb.append(String.format(" - %s\n", t));
        }
        var context = new ArrayList<>(parent.getContext());
        context.add(new Conversation("user", String.format("# Task\n%s# Query\n%s\n", query, request.getTask())));
        context.add(new Conversation("system", sb.toString()));
        node.setContext(context);
        return node;
    }

    public ReasoningTreeNode<String, List<String>>
    createRA(final String query,
             final ReasoningTreeNode<?, ?> parent,
             final PlanStep planStep) {
        var request = this.retrievalAnswerRequestTemplate.withQuery(query)
                                                         .withTemperature(this.random.nextDouble(1))
                                                         .withContext(parent.getContext());
        var retrieved = this.retrievalQualityDelegator.delegate(request, new TypeReference<List<String>>() {});
        var node = new ReasoningTreeNode<String, List<String>>(parent, planStep);
        node.setAnswer(retrieved);
        var c = 0;
        var sb = new StringBuilder();
        sb.append("[");
        for(var t : retrieved) {
            sb.append(t);
            if(c < retrieved.size() - 2) {
                sb.append(",");
            }
        }
        sb.append("]");
        var context = new ArrayList<>(parent.getContext());
        context.add(new Conversation("user", query));
        context.add(new Conversation("system", sb.toString()));
        node.setContext(context);
        return node;
    }

    public ReasoningTreeNode<Object, String>
    createRoot(final String rawQuery) {
        var planStepRoot = new PlanStep(-1, ReasoningAction.NONE, rawQuery);
        var qt = new PlanStep(-2, ReasoningAction.QT, rawQuery);
        var ra = new PlanStep(-3, ReasoningAction.RA, null);
        var sa = new PlanStep(-4, ReasoningAction.SA, null);
        var da = new PlanStep(-5, ReasoningAction.DA, null);
        var say = new PlanStep(-5, ReasoningAction.SAY, rawQuery);
        ra.addChild(sa);
        qt.addChild(ra);
        planStepRoot.addChild(qt);
        planStepRoot.addChild(da);
        planStepRoot.addChild(say);
        var root = new ReasoningTreeNode<Object, String>(null,planStepRoot);
        root.setAnswer(rawQuery);
        root.getContext().add(new Conversation("user", rawQuery));
        return root;
    }

    public ReasoningTreeNode<?, ?>
    createNode(final PlanStep planStep,
               final ReasoningTreeNode<?, ?> root) {
        var action = planStep.getAction();
        return switch (action) {
            case SAY -> this.createSAY(planStep.getContent(), root);
            case QT -> this.createQT(planStep.getContent(),root, planStep);
            case RA -> this.createRA(planStep.getContent(), root, planStep);
            case DA -> this.createDA(planStep.getContent(), root, planStep);
            case SA -> this.createSA(planStep.getContent(), root);
            case NONE -> null;
        };
    }
}
