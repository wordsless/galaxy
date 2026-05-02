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

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.github.wordsless.galaxy.core.exception.NamedEntityRecognizeException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于HanLP的本地命名实体识别器
 * 替代原本地模型方案，直接通过HanLP完成NER，轻量且无重试/Schema依赖
 */
public class NamedEntityRecognizerWithLocal implements NamedEntityRecognizer {

    // HanLP 支持的实体类型（可根据业务扩展）
    private static final List<String> SUPPORTED_ENTITY_TYPES = List.of(
            "nr", // 人名
            "ns", // 地名
            "nt", // 机构名
            "nz", // 专名
            "nw", // 作品名
            "ntc" // 公司名
    );

    /**
     * 核心NER方法：基于HanLP提取实体，封装为StructuredQuery
     * @param rawQuery 用户原始查询
     * @return 结构化查询对象（核心填充nerEntities，其他字段默认值）
     * @throws IllegalArgumentException 入参为空/空白时抛出
     * @throws NamedEntityRecognizeException HanLP执行失败时抛出
     */
    @Override
    public Map<String, String> recognize(String rawQuery) {
        // 1. 入参校验
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("原始查询字符串不能为空或空白");
        }

        try {
            // 3. HanLP 分词 + 实体识别
            List<Term> terms = StandardTokenizer.segment(rawQuery);

            // 4. 过滤出指定类型的实体
            Map<String, String> termMap = terms.stream()
                    .collect(Collectors.toMap(
                            term -> term.word,                // key：词语
                            term -> term.nature.toString(),   // value：词性
                            // 解决重复词冲突（保留第一个）
                            (oldVal, newVal) -> oldVal
                    ));
            return termMap;
        } catch (Exception e) {
            // 封装HanLP执行异常，保留上下文
            throw new NamedEntityRecognizeException(
                    "HanLP执行命名实体识别失败，原始查询：" + rawQuery,
                    e, 0, "HanLP_NER_PROCESS" // 无重试，prompt标记为固定值
            );
        }
    }

    /**
     * 扩展方法：自定义HanLP实体类型（可选）
     * @param customEntityTypes 自定义需要提取的实体类型（如"nr"/"ns"）
     */
    public void setSupportedEntityTypes(List<String> customEntityTypes) {
        Objects.requireNonNull(customEntityTypes, "自定义实体类型列表不能为空");
        SUPPORTED_ENTITY_TYPES.clear();
        SUPPORTED_ENTITY_TYPES.addAll(customEntityTypes);
    }
}