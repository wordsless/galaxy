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

package com.github.wordsless.galaxy.core.orchestrator;

import com.github.wordsless.galaxy.core.algorithm.sire.SiReEngine;
import com.github.wordsless.galaxy.core.algorithm.sire.SimilarNode;
import com.github.wordsless.galaxy.core.algorithm.sire.RelatedNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiReRAGOrchestrator extends AbstractBasicOrchestrator {

    public static final String KEY_QUERY_VECTOR = "queryVector";
    public static final String KEY_SIMILAR_ROOT = "similarTreeRoot";
    public static final String KEY_RELATED_ROOT = "relatedTreeRoot";

    private final SiReEngine engine = new SiReEngine();

    @Override
    public List<String> retrieve(Map<String, ?> context) {
        List<String> fusedResult = new ArrayList<>();

        // 1. 获取参数
        float[] queryVec = (float[]) context.get(KEY_QUERY_VECTOR);
        SimilarNode similarRoot = (SimilarNode) context.get(KEY_SIMILAR_ROOT);
        RelatedNode relatedRoot = (RelatedNode) context.get(KEY_RELATED_ROOT);

        if (queryVec == null || similarRoot == null || relatedRoot == null) {
            return fusedResult;
        }

        // ======================
        // 双路分别检索（活动图）
        // ======================
        List<String> similarResults = engine.searchSimilar(similarRoot, queryVec);
        List<String> relatedResults = engine.searchRelated(relatedRoot, queryVec);

        // ======================
        // 汇总融合（组件图）
        // ======================
        fusedResult.addAll(similarResults);
        fusedResult.addAll(relatedResults);

        return fusedResult;
    }
}