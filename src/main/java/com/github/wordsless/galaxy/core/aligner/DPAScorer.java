package com.github.wordsless.galaxy.core.aligner;

import java.util.Map;

public class DPAScorer implements CompositeScorer {

    @Override
    public double score(Map<String, Double> params) {
        double prefer = params.getOrDefault("preference", 0.0);
        double align = params.getOrDefault("alignment", 0.0);
        return 0.7 * prefer + 0.3 * align;
    }
}