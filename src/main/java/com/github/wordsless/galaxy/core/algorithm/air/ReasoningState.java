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

package com.github.wordsless.galaxy.core.algorithm.air;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.Collection;
import java.util.List;

// AirRAG推理状态：封装单步推理的核心信息
@Data
@With
@NoArgsConstructor
public class ReasoningState<T> {

    private String action;
    private String rawQuery; // 原始查询
    private String currentThought; // 当前推理步骤的思考内容
    private List<String> retrievedDocs; // 检索到的文档列表
    private PlanStep planStep;
    private T answer; // 候选答案
    private double confidence; // 答案置信度（0-1）
    private boolean isFinal; // 是否为最终推理节点

    public ReasoningState(final String action,
                          final String rawQuery,
                          final String currentThought,
                          final List<String> retrievedDocs,
                          final PlanStep planStep,
                          final T candidateAnswer,
                          final double confidence,
                          final boolean isFinal) {
        this.action         = action;
        this.rawQuery       = rawQuery;
        this.currentThought = currentThought;
        this.retrievedDocs  = retrievedDocs;
        this.planStep       = planStep;
        this.answer         = answer;
        this.confidence     = confidence;
        this.isFinal        = isFinal;
    }

    @Override
    public String toString() {
        if(this.answer instanceof Collection) {
            var sb = new StringBuilder();
            var result = (Collection) this.answer;
            sb.append('[');
            for(var item : result) {
                sb.append(item.toString());
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(']');
            return String.format("action: %s\nthought: %s\nanswer: %s\nconfidence: %f\nisFinal: %b", this.action, this.currentThought, sb.toString(), this.confidence, this.isFinal);
        } else if(this.answer instanceof String) {
            return String.format("action: %s\nthought: %s\nanswer: %s\nconfidence: %f\nisFinal: %b", this.action, this.currentThought, this.answer, this.confidence, this.isFinal);
        }
        return "";
    }
}