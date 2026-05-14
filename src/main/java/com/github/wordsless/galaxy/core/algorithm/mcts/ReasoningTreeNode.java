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

import com.github.wordsless.galaxy.core.entity.Conversation;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReasoningTreeNode<Q, A> {

    private Long id;

    private ReasoningTreeNode<?, ?> parent;
    private List<ReasoningTreeNode<?, ?>> children;
    //private List<ReasoningTreeNode<?, ?>> siblings;
    private List<Conversation> context;
    private List<ReasoningAction> candidates;
    private PlanStep planStep;
    private Q query;
    private A answer;
    private int visitCount;
    private int depth;
    private double valueSum;

    // 新建节点（根节点专用）
    public ReasoningTreeNode(PlanStep planStep) {
        this.parent = null;
        this.depth = 0;
        this.planStep = planStep;
        this.visitCount = 0;
        this.valueSum = 0.0;
        this.children = new ArrayList<>();
        this.context = new ArrayList<>();
        //this.siblings = new ArrayList<>();
        this.candidates = new ArrayList<>();
    }

    // 子节点构造（自动复制父节点上下文）
    public ReasoningTreeNode(ReasoningTreeNode<?, ?> parent, PlanStep planStep) {
        this.parent = parent;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.planStep = planStep;
        this.visitCount = 0;
        this.valueSum = 0.0;
        this.children = new ArrayList<>();
        //this.siblings = new ArrayList<>();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addChild(ReasoningTreeNode<?, ?> child) {
        if (child != null && !children.contains(child)) {
            children.add(child);
        }
    }
    /*
    public void addSibling(ReasoningTreeNode<?, ?> sibling) {
        if (sibling != null && !siblings.contains(sibling)) {
            siblings.add(sibling);
        }
    }
    */
    public void increaseVisitCount() {
        this.visitCount++;
    }

    public void updateValueSum(double value) {
        this.valueSum += value;
    }

    // 获取当前节点的平均价值 Q(s,a)
    public double getQValue() {
        return visitCount == 0 ? 0 : valueSum / visitCount;
    }

}