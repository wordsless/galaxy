/*
 * MIT License
 *
 * Copyright (c) 2024 Qiang Li (李强)
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
import lombok.Builder;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.PromptContext;
import com.github.wordsless.galaxy.core.response.QueryTransformResponse;
import com.github.wordsless.galaxy.core.response.RetrievalQualityResponse;
import com.github.wordsless.galaxy.core.response.SystemAnalysisResponse;

import java.util.Collections;
import java.util.List;

/**
 * Factory class for creating different types of reasoning tree nodes (ReasonTreeNode).
 * Each node corresponds to a specific reasoning action (SAY, RA, DA, SA, QT),
 * and encapsulates the call logic to the LLM.
 */
@Builder
public class ReasonTreeNodeFactory {

    // ChatModelDelegator for each action (injected via Builder, must not be null)
    private final ChatModelDelegator systemAnalysisDelegator;
    private final ChatModelDelegator directAnswerDelegator;
    private final ChatModelDelegator retrievalAugmentedAnswerDelegator;
    private final ChatModelDelegator queryTransformationDelegator;
    private final ChatModelDelegator summarizationAnswerDelegator;

    /**
     * Default maximum retry count for LLM calls.
     * Can be adjusted as needed; recommended value is 2~3.
     */
    @Builder.Default
    private final int defaultMaxRetryCount = 3;

    /**
     * Whether to enable JSON Schema validation.
     * Usually recommended to keep enabled to ensure correct output format.
     */
    @Builder.Default
    private final boolean enableValidation = true;

    /**
     * Creates a system analysis (SAY) node.
     * Decomposes the original question and generates a step-by-step plan.
     *
     * @param parent   parent node (can be null)
     * @param rawQuery original user question
     * @param subquery current subquery (usually same as rawQuery or rewritten)
     * @param history  conversation history
     * @return node containing SystemAnalysisResponse
     * @throws IllegalStateException if the corresponding delegator is not configured
     */
    public ReasonTreeNode<SystemAnalysisResponse> createSAY(final ReasonTreeNode<?> parent,
                                                            final String rawQuery,
                                                            final String subquery,
                                                            final List<String> history) {
        assertDelegatorNotNull(systemAnalysisDelegator, "systemAnalysisDelegator");
        var context = PromptContext.builder()
                .rawQuery(rawQuery)
                .rewritedQueries(Collections.singletonList(subquery))
                .history(history)
                .build();
        var resp = systemAnalysisDelegator.delegate(
                defaultMaxRetryCount,
                enableValidation,
                context,
                new TypeReference<SystemAnalysisResponse>() {}
        );
        var node = new ReasonTreeNode<SystemAnalysisResponse>(parent, ReasoningAction.SAY);
        node.setData(resp);
        return node;
    }

    /**
     * Creates a retrieval-augmented generation (RA) node.
     * Generates an answer with cited evidence based on retrieval results.
     *
     * @param parent   parent node
     * @param rawQuery original user question
     * @param subquery current subquery (used for retrieval)
     * @param history  conversation history
     * @return node containing RetrievalQualityResponse
     * @throws IllegalStateException if the corresponding delegator is not configured
     */
    public ReasonTreeNode<RetrievalQualityResponse> createRA(final ReasonTreeNode<?> parent,
                                                             final String rawQuery,
                                                             final String subquery,
                                                             final List<String> history) {
        assertDelegatorNotNull(retrievalAugmentedAnswerDelegator, "retrievalAugmentedAnswerDelegator");
        var context = PromptContext.builder()
                .rawQuery(rawQuery)
                .rewritedQueries(Collections.singletonList(subquery))
                .history(history)
                .build();
        var resp = retrievalAugmentedAnswerDelegator.delegate(
                defaultMaxRetryCount,
                enableValidation,
                context,
                new TypeReference<RetrievalQualityResponse>() {}
        );
        var node = new ReasonTreeNode<RetrievalQualityResponse>(parent, ReasoningAction.RA);
        node.setData(resp);
        return node;
    }

    /**
     * Creates a direct answer (DA) node.
     * Answers based solely on the model's internal knowledge, without retrieval.
     *
     * @param parent   parent node
     * @param rawQuery original user question
     * @param subquery current subquery (usually same as rawQuery)
     * @param history  conversation history
     * @return node containing RetrievalQualityResponse
     * @throws IllegalStateException if the corresponding delegator is not configured
     */
    public ReasonTreeNode<RetrievalQualityResponse> createDA(final ReasonTreeNode<?> parent,
                                                             final String rawQuery,
                                                             final String subquery,
                                                             final List<String> history) {
        assertDelegatorNotNull(directAnswerDelegator, "directAnswerDelegator");
        var context = PromptContext.builder()
                .rawQuery(rawQuery)
                .rewritedQueries(Collections.singletonList(subquery))
                .history(history)
                .build();
        var resp = directAnswerDelegator.delegate(
                defaultMaxRetryCount,
                enableValidation,
                context,
                new TypeReference<RetrievalQualityResponse>() {}
        );
        var node = new ReasonTreeNode<RetrievalQualityResponse>(parent, ReasoningAction.DA);
        node.setData(resp);
        return node;
    }

    /**
     * Creates a summarization answer (SA) node.
     * Provides a summary-style answer based on retrieved documents.
     *
     * @param parent   parent node
     * @param rawQuery original user question
     * @param subquery current subquery (used for retrieval)
     * @param history  conversation history
     * @return node containing RetrievalQualityResponse
     * @throws IllegalStateException if the corresponding delegator is not configured
     */
    public ReasonTreeNode<RetrievalQualityResponse> createSA(final ReasonTreeNode<?> parent,
                                                             final String rawQuery,
                                                             final String subquery,
                                                             final List<String> history) {
        assertDelegatorNotNull(summarizationAnswerDelegator, "summarizationAnswerDelegator");
        var context = PromptContext.builder()
                .rawQuery(rawQuery)
                .rewritedQueries(Collections.singletonList(subquery))
                .history(history)
                .build();
        var resp = summarizationAnswerDelegator.delegate(
                defaultMaxRetryCount,
                enableValidation,
                context,
                new TypeReference<RetrievalQualityResponse>() {}
        );
        var node = new ReasonTreeNode<RetrievalQualityResponse>(parent, ReasoningAction.SA);
        node.setData(resp);
        return node;
    }

    /**
     * Creates a query transformation (QT) node.
     * Generates multiple retrieval queries based on the original question
     * (e.g., query rewriting, sub-question decomposition).
     *
     * @param parent   parent node
     * @param rawQuery original user question
     * @param subquery current subquery (used as the basis for rewriting)
     * @param history  conversation history
     * @return node containing QueryTransformResponse, where the queries list can be used for subsequent retrieval
     * @throws IllegalStateException if the corresponding delegator is not configured
     */
    public ReasonTreeNode<QueryTransformResponse> createQT(final ReasonTreeNode<?> parent,
                                                           final String rawQuery,
                                                           final String subquery,
                                                           final List<String> history) {
        assertDelegatorNotNull(queryTransformationDelegator, "queryTransformationDelegator");
        var context = PromptContext.builder()
                .rawQuery(rawQuery)
                .rewritedQueries(Collections.singletonList(subquery))
                .history(history)
                .build();
        var resp = queryTransformationDelegator.delegate(
                defaultMaxRetryCount,
                enableValidation,
                context,
                new TypeReference<QueryTransformResponse>() {}
        );
        var node = new ReasonTreeNode<QueryTransformResponse>(parent, ReasoningAction.QT);
        node.setData(resp);
        return node;
    }

    /**
     * Validates that the given delegator has been injected.
     *
     * @param delegator the instance to validate
     * @param name      the field name of the delegator (used in error message)
     * @throws IllegalStateException if the delegator is null
     */
    private void assertDelegatorNotNull(Object delegator, String name) {
        if (delegator == null) {
            throw new IllegalStateException(
                    String.format("Cannot create node because '%s' is not configured in ReasonTreeNodeFactory. " +
                            "Please ensure it is set via the builder.", name)
            );
        }
    }
}
