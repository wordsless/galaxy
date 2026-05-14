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
package com.github.wordsless.galaxy.core.algorithm.mcts;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class MctsConfig {
    /**
     * UCT 算法中的探索常数，默认 sqrt(2)。
     * 增大该值会鼓励探索未充分访问的分支，减小则更依赖已有高价值分支。
     */
    private double explorationConstant = Math.sqrt(2);

    /** 默认的最小模拟深度（rollout 必须达到的最低深度） */
    private int defaultMinDepth = 2;

    /** 默认的最大模拟深度（rollout 不能超过的深度） */
    private int defaultMaxDepth = 8;

    /** 每次扩展时创建的子节点数量（固定为 1，符合标准 MCTS） */
    private int defaultSampling = 1;

    /** 默认 MCTS 主循环迭代次数 */
    private int defaultMaxIterations = 100;

    /** 连续无效扩展的最大次数，超过则提前终止迭代（防止死循环） */
    private int maxConsecutiveInvalidExpands = 10;
}