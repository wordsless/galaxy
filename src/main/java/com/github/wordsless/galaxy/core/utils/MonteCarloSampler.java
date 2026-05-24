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

import com.github.wordsless.galaxy.core.entity.EntityPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MonteCarloSampler<T extends EntityPair<?, Double>> {

    // 默认采样比例：抽取文档总数的30%
    private static final double DEFAULT_SAMPLING_RATIO = 0.3;
    // 随机数生成器（线程安全考虑，使用ThreadLocalRandom更优）
    private static final Random RANDOM = new Random();

    // 可配置参数：采样数量（优先级高于采样比例）
    private Integer sampleCount;
    // 可配置参数：是否允许重复采样同一文档
    private boolean allowDuplicate = false;

    // 无参构造器（使用默认配置）
    public MonteCarloSampler() {
    }

    // 自定义采样数量和是否允许重复
    public MonteCarloSampler(Integer sampleCount, boolean allowDuplicate) {
        this.sampleCount = sampleCount;
        this.allowDuplicate = allowDuplicate;
    }

    /**
     * 蒙特卡罗核心采样方法
     * @param entities 待采样的文档列表
     * @return 采样后的文档列表
     */
    public List<T> sampling(final List<T> entities) {
        // 空值/空列表校验
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 计算最终需要采样的数量
        int targetSampleCount = calculateTargetSampleCount(entities.size());
        if (targetSampleCount <= 0) {
            return new ArrayList<>();
        }

        // 2. 预处理：过滤掉score为负的文档（无效文档），并计算总得分（用于权重计算）
        var valid = entities.stream()
                .filter(doc -> (Double) doc.getValue() >= 0)
                .collect(Collectors.toList());

        if (valid.isEmpty()) {
            return new ArrayList<>();
        }

        double totalScore = valid.stream()
                .mapToDouble(EntityPair::getValue)
                .sum();

        // 3. 蒙特卡罗加权随机采样
        var sampled = new ArrayList<T>();
        for (int i = 0; i < targetSampleCount; i++) {
            // 生成0~总得分之间的随机数，用于命中文档
            double randomValue = RANDOM.nextDouble() * totalScore;
            double cumulativeScore = 0.0;
            T selectedEntity = null;

            // 按得分权重遍历，找到随机数命中的文档
            for (var entity : valid) {
                cumulativeScore += entity.getValue();
                if (cumulativeScore >= randomValue) {
                    selectedEntity = entity;
                    break;
                }
            }

            // 处理采样结果
            if (selectedEntity != null) {
                sampled.add(selectedEntity);
                // 如果不允许重复采样，移除已选中的文档
                if (!allowDuplicate) {
                    valid.remove(selectedEntity);
                    totalScore -= selectedEntity.getValue();
                    // 有效文档为空时提前终止采样
                    if (valid.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return sampled;
    }

    /**
     * 计算最终采样数量（优先级：自定义采样数 > 默认比例）
     * @param totalDocCount 文档总数
     * @return 目标采样数量
     */
    private int calculateTargetSampleCount(int totalDocCount) {
        if (sampleCount != null && sampleCount > 0) {
            // 自定义采样数不能超过文档总数（避免无效采样）
            return Math.min(sampleCount, totalDocCount);
        } else {
            // 使用默认比例计算采样数（至少1条，最多总数）
            int ratioBasedCount = (int) Math.ceil(totalDocCount * DEFAULT_SAMPLING_RATIO);
            return Math.max(1, Math.min(ratioBasedCount, totalDocCount));
        }
    }
}