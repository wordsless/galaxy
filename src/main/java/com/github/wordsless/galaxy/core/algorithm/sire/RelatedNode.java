package com.github.wordsless.galaxy.core.algorithm.sire;

import com.github.wordsless.galaxy.core.algorithm.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelatedNode extends TreeNode {
    private String entity;
    private float[] relationVector;
    private List<Long> linkedNodeIds;
    private String relationType;

    public RelatedNode(Long id, int depth, TreeNode parent,
                       String entity, float[] relationVector,
                       List<Long> linkedNodeIds, String relationType) {
        super(id, depth, parent);
        this.entity = entity;
        this.relationVector = relationVector;
        this.linkedNodeIds = linkedNodeIds;
        this.relationType = relationType;
    }
}