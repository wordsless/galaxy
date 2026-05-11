package com.github.wordsless.galaxy.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;

/**
 * Serializer for converting {@link ChatModelRequest} into a textual prompt
 * suitable for Large Language Model consumption.
 *
 * <p>Only non-null fields are serialized. The output format (if present) is
 * serialized as JSON with null fields omitted.</p>
 */
public class ChatModelRequestSerializer {

    // ObjectMapper for general JSON uses (e.g., simulationSequence)
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // ObjectMapper that omits null fields – used for outputFormat
    private static final ObjectMapper NON_NULL_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final boolean includeSimulationSequence;

    public ChatModelRequestSerializer() {
        this(true);
    }

    public ChatModelRequestSerializer(boolean includeSimulationSequence) {
        this.includeSimulationSequence = includeSimulationSequence;
    }

    /**
     * Serializes the given {@link ChatModelRequest} into a prompt string.
     * Fields that are null or empty are skipped entirely.
     *
     * @param request the request to serialize (must not be {@code null})
     * @return the formatted prompt string
     * @throws IllegalArgumentException if serialization of outputFormat fails
     */
    public String serialize(ChatModelRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChatModelRequest must not be null");
        }

        StringBuilder sb = new StringBuilder();

        // Role
        appendIfNotEmpty(sb, "System", request.getRole());

        // Task
        appendIfNotEmpty(sb, "Task", request.getTask());

        // Output language
        appendIfNotEmpty(sb, "Output Language", request.getOutputLanguage());

        // Simulation sequence (optional, only if non-null and non-empty)
        if (includeSimulationSequence && request.getSimulationSequence() != null && !request.getSimulationSequence().isEmpty()) {
            sb.append("## Simulation Sequence\n");
            try {
                String simJson = DEFAULT_MAPPER.writeValueAsString(request.getSimulationSequence());
                sb.append("```json\n").append(simJson).append("\n```\n\n");
            } catch (JsonProcessingException e) {
                sb.append("[Unable to serialize simulation sequence]\n\n");
            }
        }

        // Rules
        if (request.getRules() != null && !request.getRules().isEmpty()) {
            sb.append("## Rules\n");
            for (String rule : request.getRules()) {
                sb.append("- ").append(rule).append("\n");
            }
            sb.append("\n");
        }

        // Constraints
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            sb.append("## Constraints\n");
            for (String constraint : request.getConstraints()) {
                sb.append("- ").append(constraint).append("\n");
            }
            sb.append("\n");
        }

        // Context
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            sb.append("## Context\n");
            for (String ctxLine : request.getContext()) {
                sb.append(ctxLine).append("\n");
            }
            sb.append("\n");
        }

        // Query
        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            sb.append("## Query\n");
            sb.append(request.getQuery()).append("\n\n");
        }

        // Output format (only if non-null)
        if (request.getOutputFormat() != null) {
            sb.append("## Output Format\n");
            sb.append("Please respond with a JSON object matching the following structure (null fields are omitted):\n");
            try {
                // Use NON_NULL_MAPPER to exclude null-valued fields
                String formatJson = NON_NULL_MAPPER.writeValueAsString(request.getOutputFormat());
                sb.append("```json\n").append(formatJson).append("\n```\n\n");
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize outputFormat field", e);
            }
        }

        return sb.toString().trim();
    }

    /**
     * Appends a section header and content if the value is not null and not blank.
     *
     * @param sb    the StringBuilder to append to
     * @param title the section title (e.g., "System", "Task")
     * @param value the section content
     */
    private void appendIfNotEmpty(StringBuilder sb, String title, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append("## ").append(title).append("\n");
            sb.append(value).append("\n\n");
        }
    }
}