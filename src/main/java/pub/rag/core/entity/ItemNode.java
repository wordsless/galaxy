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

package pub.rag.core.entity;

import lombok.*;

import java.util.List;

/**
 * Universal tree node for reasoning, search, and RAG algorithms.
 * Supports MCTS, UCT, decision trees, recursive decomposition, and multi-hop reasoning.
 *
 * @author Qiang Li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@With
public class ItemNode {

    /**
     * Parent node in the tree.
     */
    private ItemNode parent;

    /**
     * Node label or identifier for display and tracing.
     */
    private String label;

    /**
     * Primary score or value of this node (e.g., Q-value, confidence, relevance).
     */
    private double score;

    /**
     * Actual content or data stored in this node.
     */
    private String content;

    /**
     * Node type (e.g., ROOT, SAY, QT, RA, DECISION, LEAF, etc.).
     */
    private String type;

    /**
     * Depth level of this node in the tree.
     */
    private int depth;

    /**
     * Number of times this node has been visited during tree search.
     */
    private int visitCount;

    /**
     * Child nodes of this node.
     */
    private List<ItemNode> children;

    /**
     * Cumulative total value accumulated from simulations or backpropagation.
     */
    private double totalValue;

    /**
     * Whether this node is a leaf node (no further expansion allowed).
     */
    private boolean isLeaf;

    /**
     * Whether this node requires further decomposition or expansion.
     */
    private boolean needDecompose;
}