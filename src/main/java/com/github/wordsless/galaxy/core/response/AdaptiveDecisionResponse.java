package com.github.wordsless.galaxy.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adaptive RAG Decision Response
 * Used for AT RAG routing: NO_RETRIEVAL / SINGLE_RETRIEVAL / MULTI_HOP_RETRIEVAL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveDecisionResponse extends ConfidenceResponse {
    /**
     * Final retrieval decision type
     */
    private String retrievalType;

    /**
     * Judgment reason based on NER, query complexity, entities
     */
    private String reason;

    /**
     * Next query for multi-hop retrieval
     */
    private String nextTurnQuery;
}