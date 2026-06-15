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
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SiReRAGOrchestrator extends AbstractBasicOrchestrator {

    public static final String KEY_QUERY_VECTOR = "queryVector";
    public static final String KEY_SIMILAR_ROOT = "similarTreeRoot";
    public static final String KEY_RELATED_ROOT = "relatedTreeRoot";

    private final SiReEngine engine = new SiReEngine();

    @Override
    public List<Map<Query, List<Document>>> retrieve(Context context) {
        return null;
    }
}