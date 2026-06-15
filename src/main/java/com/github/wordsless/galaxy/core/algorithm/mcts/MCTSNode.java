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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCTSNode<S> {

    private MCTSNode<S> parent;

    private List<MCTSNode<S>> children;

    private int depth;

    private int visitCount;

    private double sumValue;

    private List<Action<S>> candidateActionList;

    private S state;

    public MCTSNode(final MCTSNode<S> parent,
                    @NonNull final S state) {
        this.parent = parent;
        this.state = state;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.visitCount = 0;
        this.sumValue = 0;
        this.children = new ArrayList<>();
    }

    public void increaseVisitCount() {
        this.visitCount++;
    }

    public void updateSumValue(final double value) {
        this.sumValue += value;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean hasExpanded() {
        return candidateActionList.isEmpty();
    }

    public Action<S> nextAction(final ThreadLocalRandom random, final boolean repeatable) {
        Action<S> action = null;
        if(random != null) {
            int index = random.nextInt(candidateActionList.size());
            action = candidateActionList.get(index);
            if(!repeatable)
                this.candidateActionList.remove(index);
            return action;
        } else {
            action = candidateActionList.getLast();
            if(!repeatable) {
                candidateActionList.removeLast();
            }
            return action;
        }
    }
}
