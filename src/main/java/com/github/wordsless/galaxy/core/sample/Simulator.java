package com.github.wordsless.galaxy.core.sample;

public interface Simulator {
    double simulate(ReasoningTreeNode<?, ?> node, int minDepth, int maxDepth);
}