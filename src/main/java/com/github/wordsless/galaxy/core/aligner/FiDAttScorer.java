package com.github.wordsless.galaxy.core.aligner;

import java.util.Map;

public class FiDAttScorer implements CompositeScorer {

    @Override
    public double score(Map<String, Double> params) {
        double sim = params.getOrDefault("similarity", 0.0);
        double assoc = params.getOrDefault("association", 0.0);
        return 0.6 * sim + 0.4 * assoc;
    }
}