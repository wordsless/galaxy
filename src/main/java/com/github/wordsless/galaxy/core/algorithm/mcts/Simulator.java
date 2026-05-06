package com.github.wordsless.galaxy.core.algorithm.mcts;

import java.util.List;

public interface Simulator {
    double simulate(ReasonTreeNode<?> node, int minDepth, int maxDepth, List<String> history);
}