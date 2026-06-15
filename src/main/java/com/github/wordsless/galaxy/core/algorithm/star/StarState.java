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

package com.github.wordsless.galaxy.core.algorithm.star;

import lombok.Data;

@Data
public class StarState {

    private String context;

    // 当前步的子查询（论文：q_{t+1}）
    private final String subQuery;

    // 当前步的子答案（论文：a_{t+1}）
    private final String subAnswer;

    // 是否已经可以输出最终答案
    private final boolean isFinal;

    // 构造
    public StarState(String subQuery, String subAnswer, boolean isFinal) {
        this.subQuery = subQuery;
        this.subAnswer = subAnswer;
        this.isFinal = isFinal;
        this.context = null;
    }

    @Override
    public String toString() {
        return String.format("query: %s\nanswer: %s\n", subQuery, subAnswer);
    }
}