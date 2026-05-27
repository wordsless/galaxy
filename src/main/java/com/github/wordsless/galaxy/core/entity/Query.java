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

package com.github.wordsless.galaxy.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@With
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    @Data
    @With
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entity {
        // 实体文本
        String text;
        // 实体类型 PER/LOC/ORG
        String type;
        // 开始下标
        int start;
        // 结束下标
        int end;

        @Override
        public String toString() {
            return "{text=%s; type=%s; start=%d; end=%d}".formatted(text, type, start, end);
        }
    }

    // if sn is 0, then the query is raw query.
    private int sn;

    private String text;

    private List<Entity> NERs;

    private List<String> candidateTopics;

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("%d. %s:\n".formatted(sn, text));
        sb.append("NERs: [");
        for(var entity : NERs) {
            sb.append(entity.toString());
            sb.append(", ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]\n");
        return sb.toString();
    }
}
