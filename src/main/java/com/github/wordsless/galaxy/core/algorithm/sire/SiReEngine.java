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

package com.github.wordsless.galaxy.core.algorithm.sire;

import java.util.ArrayList;
import java.util.List;

public class SiReEngine {

    private static final float SEMANTIC_THRESHOLD = 0.6f;
    private static final float RELATION_THRESHOLD = 0.55f;

    // 余弦相似度
    public float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0f : dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    // ----------------------
    // 相似树检索
    // ----------------------
    public List<String> searchSimilar(SimilarNode root, float[] queryVec) {
        List<String> res = new ArrayList<>();
        dfsSimilar(root, queryVec, res);
        return res;
    }

    private void dfsSimilar(SimilarNode node, float[] vec, List<String> out) {
        float score = cosine(node.getSemanticVector(), vec);
        if (score >= SEMANTIC_THRESHOLD) {
            out.add("[SIM] " + node.getContent());
        }
        if (node.getChildren() == null || node.getChildren().isEmpty()) return;
        node.getChildren().forEach(c -> dfsSimilar((SimilarNode) c, vec, out));
    }

    // ----------------------
    // 关联树检索
    // ----------------------
    public List<String> searchRelated(RelatedNode root, float[] queryVec) {
        List<String> res = new ArrayList<>();
        dfsRelated(root, queryVec, res);
        return res;
    }

    private void dfsRelated(RelatedNode node, float[] vec, List<String> out) {
        float score = cosine(node.getRelationVector(), vec);
        if (score >= RELATION_THRESHOLD) {
            out.add("[REL] " + node.getEntity() + " | " + node.getRelationType());
        }
        if (node.getChildren() == null || node.getChildren().isEmpty()) return;
        node.getChildren().forEach(c -> dfsRelated((RelatedNode) c, vec, out));
    }
}