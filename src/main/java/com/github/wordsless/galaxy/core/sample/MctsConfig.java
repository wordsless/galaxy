/*
 * MIT License
 * Copyright (c) 2025 Qiang Li (李强)
 *
 * MCTS 超参数配置类，支持通过 application.yml 动态调整。
 * 意图：解耦硬编码常量，便于调优探索/利用平衡及深度控制。
 */
package com.github.wordsless.galaxy.core.sample;

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