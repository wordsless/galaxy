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

package com.github.wordsless.galaxy.core;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.wordsless.galaxy.core.algorithm.mcts.ReasoningAction;
import com.github.wordsless.galaxy.core.entity.Conversation;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Represents a complete request to a Large Language Model (LLM) combining
 * prompt engineering components (role, task, rules) with generation parameters.
 *
 * @author Qiang Li
 * @since 1.0
 */
@Builder(toBuilder = true) // Allow copying with modifications
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // Map fields like `topP` -> `top_p`
@Data
@With
public class ChatModelRequest {

    // ==================== Core Prompt Components ====================

    /** The LLM model identifier (e.g., "gpt-4", "claude-3-opus") */
    @NonNull
    String model;

    /** Role definition (e.g., "You are a helpful assistant") */
    String role;

    /** Task description (e.g., "Summarize the following text") */
    String task;

    /** Desired output language (e.g., "English", "Chinese") */
    String outputLanguage;

    /**
     * Optional simulation sequence data (e.g., steps of a recursive simulation,
     * synthetic examples, or hypothetical execution traces).
     * This is distinct from real `context` which holds actual conversation history
     * or genuine inference tree nodes.
     */
    List<Map<ReasoningAction, Double>> simulationSequence;

    /** The raw query or input text */
    String rawQuery;

    Map<String, String> NERs;

    List<String> rewritedMultiQueries;

    List<String> topics;

    /** List of behavioral rules the LLM must follow */
    List<String> rules;

    /** Additional constraints (e.g., length limits, forbidden content) */
    List<String> constraints;

    List<String> documents;

    /** Relevant contextual information or conversation history */
    List<Conversation> context;

    /** Structured output format specification;*/
    String outputFormat;

    // ==================== Generation Parameters ====================

    /**
     * Sampling temperature (0.0 - 2.0). Higher = more random, lower = more deterministic.
     * Default: 0.7
     */
    @Builder.Default
    double temperature = 0.7;

    /**
     * Nucleus sampling threshold (0.0 - 1.0). Only tokens with cumulative probability >= topP are considered.
     * Default: 1.0 (disabled)
     */
    @Builder.Default
    double topP = 1.0;

    /** Maximum number of tokens to generate. null = model's maximum allowed. */
    Integer maxTokens;

    /**
     * Presence penalty (-2.0 - 2.0). Positive values discourage repeating tokens already present.
     * Default: 0.0
     */
    @Builder.Default
    double presencePenalty = 0.0;

    /**
     * Frequency penalty (-2.0 - 2.0). Positive values discourage frequent token repetition.
     * Default: 0.0
     */
    @Builder.Default
    double frequencyPenalty = 0.0;

    /** Number of completion choices to generate per prompt. Default: 1 */
    @Builder.Default
    int n = 1;

    /** Whether to stream partial results as they are generated. Default: false */
    @Builder.Default
    boolean stream = false;

    /** Whether to return log probabilities of the generated tokens. Default: false */
    @Builder.Default
    boolean logprobs = false;

    /** Whether to echo the prompt in the response. Default: false */
    @Builder.Default
    boolean echo = false;

    /** Deterministic seed for reproducible results (not supported by all providers). */
    Integer seed;

    /**
     * Number of completions generated server-side to choose the best one.
     * Requires provider support. Default: 1 (same as n)
     */
    @Builder.Default
    Integer bestOf = 1;

    /**
     * Token bias adjustments: map token IDs to bias values (-100 to 100).
     * Positive makes token more likely, negative less likely.
     */
    Map<String, Integer> logitBias;

    // ==================== Builder Custom Logic & Validation ====================

    /**
     * Custom builder constructor that validates parameter ranges.
     * Because we use @Builder, we define a private constructor and then
     * implement a static builder() method that returns a custom builder.
     * Alternatively, Lombok's @Builder can be combined with @Builder.Default
     * and runtime validation in a custom setter. Here we use the "build" method
     * approach by creating a custom builder class. For simplicity and clarity,
     * we implement a static inner Builder class manually instead of using Lombok's
     * auto-generated one. However, to keep the code concise and leverage Lombok,
     * we can keep Lombok @Builder and add validation via @Builder.MethodVisibility
     * and a private constructor. Below is a hybrid approach:
     *
     * We declare the class as @Builder, but we also add a private constructor
     * with validation called by the generated builder. Unfortunately, Lombok
     * doesn't call a user-defined constructor automatically. Another clean way:
     * Use @Builder(toBuilder = true) and then create a static method that
     * validates after building. We'll provide a utility method `validate()`.
     *
     * For thoroughness, we add a separate validation method that can be called
     * explicitly by the client or internally by a wrapper.
     */

    /**
     * Validates the request parameters according to API constraints.
     * Throws IllegalArgumentException if any parameter is out of allowed range.
     */
    public void validate() {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        if (topP < 0.0 || topP > 1.0) {
            throw new IllegalArgumentException("topP must be between 0.0 and 1.0");
        }
        if (maxTokens != null && maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (presencePenalty < -2.0 || presencePenalty > 2.0) {
            throw new IllegalArgumentException("presencePenalty must be between -2.0 and 2.0");
        }
        if (frequencyPenalty < -2.0 || frequencyPenalty > 2.0) {
            throw new IllegalArgumentException("frequencyPenalty must be between -2.0 and 2.0");
        }
        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }
        if (bestOf != null && bestOf < 1) {
            throw new IllegalArgumentException("bestOf must be at least 1 if provided");
        }
        if (logitBias != null) {
            for (Integer bias : logitBias.values()) {
                if (bias < -100 || bias > 100) {
                    throw new IllegalArgumentException("logitBias values must be between -100 and 100");
                }
            }
        }
    }

    // The @Builder annotation will generate a builder class.
    // Clients should call .validate() after building if they want strict checking.
}