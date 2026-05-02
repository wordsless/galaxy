package pub.rag.core.response;

import lombok.*;

import java.util.List;

/**
 * QT (Query Transformation) 动作的输出。
 * 包含 LLM 对当前上下文进行分析后生成的检索查询列表，
 * 每个查询将作为后续检索动作（如 RA）的输入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class QueryTransformResponse extends ConfidenceResponse{

    /** 生成查询时所基于的原始问题或上下文摘要（便于追溯） */
    private String originalContext;

    /** 生成的检索查询列表 */
    private List<String> queries;

    /**
     * 可选：查询生成的理由或策略说明（例如为什么这样重写），
     * 用于调试或下游决策，可为空。
     */
    @Builder.Default
    private String generationNote = "";
}