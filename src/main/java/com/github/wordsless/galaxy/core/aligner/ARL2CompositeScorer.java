package com.github.wordsless.galaxy.core.aligner;

import java.util.Map;

public class ARL2CompositeScorer implements CompositeScorer {

    @Override
    public double score(Map<String, Double> params) {
        double rel = params.getOrDefault("relevance", 0.0);
        double useful = params.getOrDefault("usefulness", 0.0);
        return 0.5 * rel + 0.5 * useful;
    }
}