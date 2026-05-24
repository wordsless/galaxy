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

package com.github.wordsless.galaxy.core.preprocessor;

import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.exception.NamedEntityRecognizeException;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于HanLP的本地命名实体识别器
 * 替代原本地模型方案，直接通过HanLP完成NER，轻量且无重试/Schema依赖
 * 核心能力：提取查询文本中的人名(PER)、地名(LOC)、机构名(ORG)等实体，封装为Query.Entity结构
 */
public class NamedEntityRecognizerWithLocal implements NamedEntityRecognizer {

    // HanLP词性 → 标准实体类型映射（nr=人名/PER，ns=地名/LOC，nt=机构名/ORG）
    private static final Map<String, String> ENTITY_TYPE_MAPPING = Map.of(
            "nr", "PER",  // 人名 → 标准PER类型
            "ns", "LOC",  // 地名 → 标准LOC类型
            "nt", "ORG",  // 机构名 → 标准ORG类型
            "nz", "ORG",  // 专名 → 归为ORG（可根据业务调整）
            "nw", "ORG",  // 作品名 → 归为ORG（可根据业务调整）
            "ntc", "ORG"  // 公司名 → 归为ORG
    );

    /**
     * 核心NER方法：基于HanLP提取实体，填充到Context的RawQuery中
     * @param context 上下文对象，包含原始查询和待填充的NER结果
     * @throws IllegalArgumentException 入参为空/空白时抛出
     * @throws NamedEntityRecognizeException HanLP执行失败时抛出
     */
    @Override
    public void process(Context context) {
        // 1. 入参校验
        if (context == null) {
            throw new IllegalArgumentException("Context上下文不能为空");
        }
        Query rawQuery = context.getQuery();
        if (rawQuery == null || rawQuery.getQuery() == null || rawQuery.getQuery().isBlank()) {
            throw new IllegalArgumentException("原始查询字符串不能为空或空白");
        }
        String queryText = rawQuery.getQuery().trim();

        try {
            // 2. HanLP 分词 + 实体识别（带位置信息）
            List<Term> terms = StandardTokenizer.segment(queryText);

            // 3. 解析实体：过滤支持的类型，封装为Query.Entity
            List<Query.Entity> nerEntities = parseEntities(terms, queryText);

            // 4. 将识别结果填充回RawQuery
            rawQuery.setNERs(nerEntities);

        } catch (IllegalArgumentException e) {
            // 入参相关异常直接抛出
            throw e;
        } catch (Exception e) {
            // 封装HanLP执行异常，保留上下文
            throw new NamedEntityRecognizeException(
                    "HanLP执行命名实体识别失败，原始查询：" + rawQuery.getQuery(),
                    e, 0, "HanLP_NER_PROCESS" // 无重试，prompt标记为固定值
            );
        }
    }

    /**
     * 解析HanLP分词结果，提取实体并封装为Query.Entity
     * @param terms HanLP分词结果
     * @param queryText 原始查询文本
     * @return 标准化的实体列表
     */
    private List<Query.Entity> parseEntities(List<Term> terms, String queryText) {
        List<Query.Entity> entities = new ArrayList<>();
        int currentIndex = 0; // 记录当前字符的位置偏移

        for (Term term : terms) {
            String word = term.word;
            String nature = term.nature.toString();

            // 跳过非支持的实体类型
            if (!ENTITY_TYPE_MAPPING.containsKey(nature)) {
                currentIndex += word.length();
                continue;
            }

            // 计算实体在原始文本中的起止下标
            int start = currentIndex;
            int end = currentIndex + word.length() - 1; // 闭区间[start, end]

            // 封装实体对象
            Query.Entity entity = new Query.Entity();
            entity.setText(word);
            entity.setType(ENTITY_TYPE_MAPPING.get(nature));
            entity.setStart(start);
            entity.setEnd(end);

            entities.add(entity);

            // 更新位置偏移
            currentIndex += word.length();
        }

        return entities;
    }
}