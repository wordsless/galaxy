package pub.rag.core.response;

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
public class SystemAnalysisResponse extends ConfidenceResponse{

    /** 原始问题文本（保留以便回溯） */
    private String originalQuestion;

    /** 计划步骤列表，按执行顺序排列 */
    private List<PlanStep> steps;

    // ------------------- 内部类 -------------------

    /**
     * 单个计划步骤（子问题或推理子步骤）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanStep {

        /** 步骤编号（1‑based，反映 SAY 输出中的顺序） */
        private int index;

        /** 子问题或步骤的文本描述（作为后续动作的输入） */
        private String description;

        /**
         * 可选：本步骤建议的后续动作类型提示。
         * 例如 "RA"、"DA"、"QT" 等；
         * 若为 null 或空字符串，表示由调度器自行决定。
         */
        @Builder.Default
        private String suggestedActionHint = "";
    }
}