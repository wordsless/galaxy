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

package com.github.wordsless.galaxy.core.algorithm.mcts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;

import java.util.Arrays;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReasonTreeNode<T> extends TreeNode<T> {

    protected ReasoningAction action;

    protected int visitCount;

    protected double valueSum;

    protected boolean selected;

    protected boolean simulated;

    protected boolean backpropagated;

    protected List<ReasoningAction> candidates;

    public ReasonTreeNode(final ReasonTreeNode<?> parent, final ReasoningAction action) {
        super(parent == null ? 0 : parent.getDepth() + 1, parent);
        this.action = action;
        this.visitCount = 0;
        this.valueSum = 0;
        this.selected = false;
        this.candidates = Arrays.asList(ReasoningAction.values());
        this.candidates.remove(action);
    }

    public boolean hasCompletelyExpanded() {
        return candidates.isEmpty();
    }

    public ReasoningAction nextAction(boolean needRemove) {
        var next = candidates.getFirst();
        if(needRemove)
            candidates.removeFirst();
        return next;
    }

}
