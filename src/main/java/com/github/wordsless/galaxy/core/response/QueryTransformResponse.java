package com.github.wordsless.galaxy.core.response;

import lombok.*;

import java.util.List;

/**
 * QT (Query Transformation) 动作的输出。
 * 包含 LLM 对当前上下文进行分析后生成的检索查询列表，
 * 每个查询将作为后续检索动作（如 RA）的输入。
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class QueryTransformResponse extends AbstractBasicResponse {

    /** 生成的检索查询列表 */
    private List<String> transformedQueries;
}