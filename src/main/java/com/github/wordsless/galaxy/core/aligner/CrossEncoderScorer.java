package com.github.wordsless.galaxy.core.aligner;

import java.util.Map;

public class CrossEncoderScorer implements CompositeScorer {

    @Override
    public double score(Map<String, Double> params) {
        return params.getOrDefault("cross_score", 0.0);
    }
}