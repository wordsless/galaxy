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

package com.github.wordsless.galaxy.core.evaluators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.ChatModelRequest;
import com.github.wordsless.galaxy.core.Evaluator;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * A chat model-based evaluator implementation that supports three core metrics:
 * Usefulness, Faithfulness, and Completeness.
 *
 * This evaluator constructs a chat model request using predefined templates and context data,
 * then delegates the request to a chat model delegator to get metric scores.
 * Marked as Spring Component for dependency injection management.
 */
@Component
public class BasedChatModelEvaluator implements Evaluator {

    // Unique label for identifying this evaluator (e.g., "usefulness_evaluator")
    private final String label;

    // Predefined request template with placeholders for raw query, documents, and answer
    private final ChatModelRequest template;

    // Delegator for sending requests to chat model and parsing response to metric scores
    private final ChatModelDelegator<Map<String, Double>> delegator;

    /**
     * Constructor for BasedChatModelEvaluator with mandatory dependencies.
     * All parameters are non-null to ensure the evaluator works correctly.
     *
     * @param label Unique identifier for the evaluator instance
     * @param template Predefined ChatModelRequest template with placeholders
     * @param delegator Delegator for handling chat model communication and response parsing
     */
    public BasedChatModelEvaluator(@NonNull final String label,
                                   @NonNull final ChatModelRequest template,
                                   @NonNull final ChatModelDelegator<Map<String, Double>> delegator) {
        this.label = label;
        this.template = template;
        this.delegator = delegator;
    }

    /**
     * Executes evaluation by building a request from context data and delegating to chat model.
     * Extracts raw query, documents, and answer from context, populates the template,
     * then sends the request to get metric scores.
     *
     * @param context Input data map containing:
     *                - "RawQuery": Original user query (String)
     *                - "Docs": List of reference documents (List<String>)
     *                - "Answer": Generated answer to evaluate (String)
     * @return Map of metric names (e.g., "usefulness") to their double scores (0-1 range)
     * @throws ClassCastException If context values are not of the expected type
     * @throws NullPointerException If required context keys are missing
     */
    @Override
    public Map<String, Double> evaluate(Map<String, ?> context) {
        // Extract core data from evaluation context
        var rawQuery = (String) context.get("RawQuery");
        var docs = (List<String>) context.get("Docs");
        var answer = (String) context.get("Answer");
        var constraints = (List<String>) context.get("Constraints");

        // Populate the predefined template with context data
        var request = this.template.withRawQuery(rawQuery)
                                   .withDocuments(docs)
                                   .withConstraints(constraints)
                                   .withAnswer(answer);

        // Delegate request to chat model and return parsed metric scores
        return delegator.delegate(request, new TypeReference<Map<String, Double>>() {});
    }
}