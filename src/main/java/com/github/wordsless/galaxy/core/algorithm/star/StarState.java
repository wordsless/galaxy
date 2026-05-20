package com.github.wordsless.galaxy.core.algorithm.star;

import lombok.Data;

@Data
public class StarState {

    private String context;

    // 当前步的子查询（论文：q_{t+1}）
    private final String subQuery;

    // 当前步的子答案（论文：a_{t+1}）
    private final String subAnswer;

    // 是否已经可以输出最终答案
    private final boolean isFinal;

    // 构造
    public StarState(String subQuery, String subAnswer, boolean isFinal) {
        this.subQuery = subQuery;
        this.subAnswer = subAnswer;
        this.isFinal = isFinal;
        this.context = null;
    }

    @Override
    public String toString() {
        return String.format("query: %s\nanswer: %s\n", subQuery, subAnswer);
    }
}