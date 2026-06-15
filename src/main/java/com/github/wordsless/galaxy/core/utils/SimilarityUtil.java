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

package com.github.wordsless.galaxy.core.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 HanLP 分词的 Query 相似度计算
 * 用于：改写 Query 排序 → 选出 TOP1 最优查询
 */
public final class SimilarityUtil {

    /**
     * 计算两个 query 的相似度 (0~1)
     */
    public static double calculateSimilarity(String query1, String query2) {
        if (query1 == null || query2 == null) return 0.0;
        if (query1.isBlank() || query2.isBlank()) return 0.0;

        // 1. HanLP 分词（自动过滤停用词、标点）
        Set<String> words1 = tokenizeToSet(query1);
        Set<String> words2 = tokenizeToSet(query2);

        // 2. 计算 Jaccard 相似度（集合交集/并集）
        return jaccard(words1, words2);
    }

    /**
     * 从多个改写 Query 中选出【最相似于原始Query】的 TOP1
     */
    public static String getBestRewrittenQuery(String rawQuery, List<String> rewrittenList) {
        if (rewrittenList == null || rewrittenList.isEmpty()) {
            return rawQuery;
        }

        double maxScore = -1;
        String bestQuery = rawQuery;

        for (String rewritten : rewrittenList) {
            double score = calculateSimilarity(rawQuery, rewritten);
            if (score > maxScore) {
                maxScore = score;
                bestQuery = rewritten;
            }
        }
        return bestQuery;
    }

    // ==================== 内部工具方法 ====================

    /**
     * HanLP 分词 → 转成 Set（去重）
     */
    private static Set<String> tokenizeToSet(String text) {
        List<Term> termList = HanLP.segment(text);
        return termList.stream()
                .map(term -> term.word.trim())
                .filter(word -> word.length() > 1)  // 过滤单字（可选优化）
                .collect(Collectors.toSet());
    }

    /**
     * Jaccard 相似度公式
     */
    private static double jaccard(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}
