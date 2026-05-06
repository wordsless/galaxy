package com.github.wordsless.galaxy.core.algorithm.mcts;

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import com.github.wordsless.galaxy.core.response.ConfidenceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReasonTreeNode<T extends ConfidenceResponse> extends TreeNode<T> {

    private final ReasoningAction action;
    private int visitCount;
    private double valueSum;
    private final List<ReasoningAction> candidates; // untried actions

    public ReasonTreeNode(ReasoningAction action, List<ReasoningAction> candidates) {
        super(0, null);
        this.action = action;
        this.visitCount = 0;
        this.valueSum = 0.0;
        this.candidates = new ArrayList<>(candidates);
    }

    public ReasonTreeNode(ReasonTreeNode<?> parent, ReasoningAction action, List<ReasoningAction> candidates) {
        super(parent.getDepth() + 1, parent);
        this.action = action;
        this.visitCount = 0;
        this.valueSum = 0.0;
        this.candidates = new ArrayList<>(candidates);
    }

    public boolean isFullyExpanded() {
        return candidates.isEmpty();
    }

    public ReasoningAction nextAction() {
        if (candidates.isEmpty()) return null;
        return candidates.remove(candidates.size() - 1);
    }

    // Note: parent depth is used; no duplicate depth field
}
