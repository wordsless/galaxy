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

package com.github.wordsless.galaxy.core.algorithm.tree;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TreeNode<T> {

    protected int depth;

    protected TreeNode<?> parent;

    protected List<TreeNode<?>> children;

    protected List<TreeNode<?>> siblings;

    protected T data;

    public TreeNode(final int depth, final TreeNode<?> parent) {
        this.data = null;
        this.children = new ArrayList<>();
        this.siblings = new ArrayList<>();
        this.depth = depth;
        this.parent = parent;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    private int addSibling(final TreeNode<?> node) {
        siblings.add(node);
        return siblings.size();
    }

    public int addChild(final TreeNode<?> node) {
        children.add(node);
        for(var s : siblings) {
            if(s.equals(node))
                continue;
            node.addSibling(s);
            s.addSibling(node);
        }
        siblings.add(node);
        return children.size();
    }

}
