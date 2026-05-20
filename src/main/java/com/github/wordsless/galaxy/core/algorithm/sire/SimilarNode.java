package com.github.wordsless.galaxy.core.algorithm.sire;

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SimilarNode extends TreeNode {
    private String content;
    private float[] semanticVector;
    private String docId;

    public SimilarNode(Long id, int depth, TreeNode parent, String content, float[] semanticVector, String docId) {
        super(id, depth, parent);
        this.content = content;
        this.semanticVector = semanticVector;
        this.docId = docId;
    }
}