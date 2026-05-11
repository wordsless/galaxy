package com.github.wordsless.galaxy.core.response;

import com.github.wordsless.galaxy.core.sample.PlanStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SAY (System Analysis) 动作的输出。
 * 包含 LLM 对原始问题进行分析后产生的步骤计划，
 * 每个步骤为一个独立的子问题或执行项，后续动作将针对单个步骤展开。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnalysisResponse extends AbstractBasicResponse {

    /** 计划步骤列表，按执行顺序排列 */
    private List<PlanStep> steps;

}