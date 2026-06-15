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

package com.github.wordsless.galaxy.core.preprocessor.rewriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.QueryRewriter;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;

import java.util.Collections;
import java.util.List;

/**
 * 查询重写处理器：将原始查询通过大模型重写为更精准的多个查询语句
 * 用于提升检索/问答的准确性
 */
public class SingleHopQueryRewriter implements QueryRewriter<List<Query>> {

    private final ChatModelDelegator<List<Query>> chatModelDelegator;
    private final ChatModelRequest queryRewriteRequest;

    /**
     * 构造函数注入依赖
     *
     * @param chatModelDelegator 大模型请求委托器
     * @param queryRewriteRequest 查询重写专用请求模板
     */
    public SingleHopQueryRewriter(final ChatModelDelegator<List<Query>> chatModelDelegator,
                                  final ChatModelRequest queryRewriteRequest) {
        this.chatModelDelegator = chatModelDelegator;
        this.queryRewriteRequest = queryRewriteRequest;
    }

    /**
     * 执行查询重写
     *
     * @param context 上下文（包含原始查询）
     * @return 重写后的查询列表，若上下文/原始查询为空则返回空列表
     */
    @Override
    public List<Query> rewrite(final Context context) {
        // 空值校验
        if (context == null || context.getQuery() == null) {
            return Collections.emptyList();
        }

        var rawQuery = context.getQuery();
        var request = this.queryRewriteRequest.withQuery(rawQuery);
        try {
            return chatModelDelegator.delegate(request, new TypeReference<List<Query>>() {}, context);
        } catch (Exception e) {
            // 异常时返回包含原始查询的列表，保证流程不中断
            return Collections.singletonList(rawQuery);
        }
    }
}
