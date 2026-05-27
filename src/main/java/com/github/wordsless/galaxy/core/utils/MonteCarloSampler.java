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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 泛型蒙特卡洛采样器（带 Token 总数限制）
 * @param <T> 采样数据类型
 */
public class MonteCarloSampler<T> {

    private static final Random RANDOM = new Random();

    public MonteCarloSampler() {}

    // ====================== 核心修改：入参改为 Collection<T> 并调整采样逻辑 ======================
    /**
     * 蒙特卡洛采样（限制最大 token 数量）
     * @param dataset 待采样的数据集
     * @param maxTokens 最大允许生成的 token 总数
     * @param tokenCounter 计算单个样本占用多少 token 的函数
     * @return 采样结果列表（总 token ≤ maxTokens）
     */
    public List<T> sample(
            Collection<T> dataset,
            long maxTokens,
            Function<T, Long> tokenCounter) {
        // 空数据集直接返回空列表
        if (dataset == null || dataset.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> samples = new ArrayList<>();
        int totalTokens = 0;
        // 将数据集转为可随机访问的列表，并创建待采样的副本（避免修改原数据集）
        List<T> remainingData = new ArrayList<>(dataset);

        while (totalTokens < maxTokens && !remainingData.isEmpty()) {
            // 随机选择一个样本
            int randomIndex = RANDOM.nextInt(remainingData.size());
            T sample = remainingData.remove(randomIndex);
            var tokens = tokenCounter.apply(sample);

            // 超过上限则停止采样
            if (totalTokens + tokens > maxTokens) {
                break;
            }

            samples.add(sample);
            totalTokens += tokens;
        }

        return samples;
    }

    /**
     * 计算采样期望（泛型通用）
     */
    public double calculateExpectation(
            Supplier<T> sampleSupplier,
            int maxTokens,
            Function<T, Integer> tokenCounter,
            Function<T, Number> mapper
    ) {
        double sum = 0.0;
        int totalTokens = 0;
        int count = 0;

        while (totalTokens < maxTokens) {
            T sample = sampleSupplier.get();
            int tokens = tokenCounter.apply(sample);

            if (totalTokens + tokens > maxTokens) {
                break;
            }

            sum += mapper.apply(sample).doubleValue();
            totalTokens += tokens;
            count++;
        }

        return count == 0 ? 0 : sum / count;
    }

    /**
     * 计算概率
     */
    public double calculateProbability(
            Supplier<T> sampleSupplier,
            int maxTokens,
            Function<T, Integer> tokenCounter,
            Function<T, Boolean> condition
    ) {
        int hit = 0;
        int total = 0;
        int totalTokens = 0;

        while (totalTokens < maxTokens) {
            T sample = sampleSupplier.get();
            int tokens = tokenCounter.apply(sample);

            if (totalTokens + tokens > maxTokens) {
                break;
            }

            total++;
            totalTokens += tokens;
            if (condition.apply(sample)) hit++;
        }

        return total == 0 ? 0 : (double) hit / total;
    }
}