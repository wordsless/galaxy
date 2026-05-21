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

package com.github.wordsless.galaxy.core.aligner;

import com.github.wordsless.galaxy.core.Aligner;
import com.github.wordsless.galaxy.core.entity.Document;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 默认对齐器实现，基于蒙特卡罗采样和多轮评分优化文档对齐
 */
public class DefaultAligner implements Aligner {

    // 线程安全的随机数生成器
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    // 批量对齐服务（需外部注入）
    private BatchAlignService service;
    // 复合评分器（需外部注入）
    private CompositeScorer scorer;

    // 默认蒙特卡罗迭代次数
    private static final int DEFAULT_MONTE_CARLO_ITERATIONS = 1000;

    public DefaultAligner(BatchAlignService service, CompositeScorer scorer) {
        this.service = service;
        this.scorer = scorer;
    }

    /**
     * 蒙特卡罗采样：从候选文档中采样，满足token总数不超过上限
     * 优化点：
     * 1. 提前过滤无效文档，减少循环次数
     * 2. 增加最优解缓存，提前终止无效迭代
     * 3. 优化随机洗牌逻辑，提升随机性
     *
     * @param docs            候选文档集合（不可为null）
     * @param tokenCountLimit token总数上限（非负）
     * @return 采样后的文档列表（token总数≤tokenCountLimit），永远返回非null列表
     */
    public List<Document> sampling(final List<Document> docs, long tokenCountLimit) {
        // 边界条件处理：空输入或非法上限直接返回空列表
        if (docs == null || docs.isEmpty() || tokenCountLimit <= 0) {
            return Collections.emptyList();
        }

        // 步骤1：预处理 - 过滤无效文档（tokenCount为null/负数/超过上限）
        List<Document> validDocs = docs.stream()
                .filter(Objects::nonNull) // 过滤null文档
                .filter(doc -> doc.getTokenCount() != null && doc.getTokenCount() > 0) // 过滤tokenCount无效的文档
                .filter(doc -> doc.getTokenCount() <= tokenCountLimit) // 过滤超过token上限的单篇文档
                .collect(Collectors.toList());

        // 无有效文档时返回空列表
        if (validDocs.isEmpty()) {
            return Collections.emptyList();
        }

        // 蒙特卡罗核心：多次随机采样，找到最接近上限的组合
        long bestTotalToken = 0;
        List<Document> bestSample = new ArrayList<>();
        int monteCarloIterations = DEFAULT_MONTE_CARLO_ITERATIONS;

        for (int i = 0; i < monteCarloIterations; i++) {
            List<Document> currentSample = new ArrayList<>();
            long currentTotalToken = 0;

            // 随机打乱文档顺序（模拟随机采样）
            List<Document> shuffledDocs = new ArrayList<>(validDocs);
            shuffleList(shuffledDocs);

            // 累加文档，直到接近token上限
            for (Document doc : shuffledDocs) {
                long docToken = doc.getTokenCount();
                if (currentTotalToken + docToken <= tokenCountLimit) {
                    currentSample.add(doc);
                    currentTotalToken += docToken;
                }

                // 达到上限时提前终止当前迭代
                if (currentTotalToken == tokenCountLimit) {
                    break;
                }
            }

            // 更新最优采样结果
            if (currentTotalToken > bestTotalToken) {
                bestTotalToken = currentTotalToken;
                bestSample = new ArrayList<>(currentSample);

                // 提前终止所有迭代：已找到完美匹配上限的组合
                if (bestTotalToken == tokenCountLimit) {
                    break;
                }
            }
        }

        return bestSample;
    }

    /**
     * 自定义列表洗牌方法（线程安全，避免依赖Collections.shuffle）
     *
     * @param list 待洗牌的列表（不可为null）
     */
    private void shuffleList(List<Document> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            // 交换元素
            Document temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    /**
     * 计算文档多轮评分的平均值
     * 修复点：原逻辑错误地使用values.size()作为循环次数，改为使用scores.size()
     *
     * @param doc 待计算的文档（不可为null）
     * @return 各维度的平均评分，永远返回非null的Map
     */
    private Map<String, Double> calculateAverage(Document doc) {
        Map<String, Double> avgs = new HashMap<>();
        if (doc == null || doc.getMultiTurnValues() == null || doc.getMultiTurnValues().isEmpty()) {
            return avgs;
        }

        Map<String, List<Double>> multiTurnValues = doc.getMultiTurnValues();
        for (Map.Entry<String, List<Double>> entry : multiTurnValues.entrySet()) {
            String key = entry.getKey();
            List<Double> scores = entry.getValue();

            if (scores == null || scores.isEmpty()) {
                avgs.put(key, 0.0);
                continue;
            }

            // 计算有效分数的平均值
            double sum = scores.stream()
                    .filter(Objects::nonNull) // 过滤null分数
                    .mapToDouble(Double::doubleValue)
                    .sum();
            avgs.put(key, sum / scores.size());
        }

        return avgs;
    }

    /**
     * 核心对齐方法：多轮蒙特卡罗采样 + 服务调用 + 评分计算 + 排序
     * 优化点：
     * 1. 增加参数校验
     * 2. 修复TreeSet初始化的潜在问题
     * 3. 优化循环逻辑，减少空指针风险
     * 4. 增加异常防护（可选，根据业务需求）
     *
     * @param docs            候选文档列表（不可为null）
     * @param tokenCountLimit token总数上限（非负）
     * @param iterCount       迭代次数（正整数）
     * @return 按评分排序后的文档列表，永远返回非null列表
     */
    @Override
    public List<Document> align(final List<Document> docs, final long tokenCountLimit, final int iterCount) {
        // 参数校验
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }
        if (tokenCountLimit <= 0 || iterCount <= 0) {
            return new ArrayList<>(docs); // 返回原列表的拷贝
        }
        if (service == null) {
            throw new IllegalStateException("BatchAlignService未初始化");
        }
        if (scorer == null) {
            throw new IllegalStateException("CompositeScorer未初始化");
        }

        // 步骤1：构建候选文档索引（需Document实现Comparable接口）
        TreeSet<Document> index = new TreeSet<>(docs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        // 步骤2：多轮蒙特卡罗采样与对齐
        int currentIter = 0;
        while (currentIter < iterCount) {
            try {
                // 蒙特卡罗采样
                List<Document> sampled = sampling(docs, tokenCountLimit);
                if (sampled.isEmpty()) {
                    currentIter++;
                    continue;
                }

                // 调用微服务进行文档对齐
                List<Document> aligned = service.call(sampled);
                if (aligned == null || aligned.isEmpty()) {
                    currentIter++;
                    continue;
                }

                // 更新文档评分
                for (Document item : aligned) {
                    if (item == null) {
                        continue;
                    }
                    Document doc = index.ceiling(item);
                    if (doc != null) {
                        doc.setMultiTurnValues(item.getMultiTurnValues());
                    }
                }
            } catch (Exception e) {
                // 异常处理：记录日志（建议添加日志框架），继续下一轮迭代
                // log.error("第{}轮对齐失败", currentIter, e);
                currentIter++;
                continue;
            }
            currentIter++;
        }

        // 步骤3：计算最终评分
        for (Document doc : docs) {
            if (doc == null) {
                continue;
            }
            Map<String, Double> parameters = calculateAverage(doc);
            doc.setScore(scorer.score(parameters));
        }

        // 步骤4：按评分排序（需Document实现compareTo方法）
        List<Document> result = new ArrayList<>(docs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        result.sort(Document::compareTo);

        return result;
    }
}