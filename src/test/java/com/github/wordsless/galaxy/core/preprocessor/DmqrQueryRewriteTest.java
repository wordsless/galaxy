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

package com.github.wordsless.galaxy.core.preprocessor;

import com.github.wordsless.galaxy.core.config.BasicConfig;
import com.github.wordsless.galaxy.core.config.ChatModelRequestConfig;
import com.github.wordsless.galaxy.core.config.QueryRewriterConfig;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.preprocessor.rewriter.DmqrQueryRewrite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        BasicConfig.class,
        ChatModelRequestConfig.class,
        QueryRewriterConfig.class
})
@TestPropertySource(properties = {
        "decision.api.key=${DECISION_API_KEY}",
        "decision.base.url=https://api.deepseek.com",
        "decision.model.name=deepseek-v4-pro",
        "decision.model.temperature=0.7",
        "chat_model.request.retry=3"
})
public class DmqrQueryRewriteTest {

    // 真实从 Spring 拿到的真实 Bean
    @Autowired
    private DmqrQueryRewrite dmqrQueryRewrite;

    @Test
    public void test_BeanLoadedSuccess() {
        assertNotNull(dmqrQueryRewrite);
    }

    @Test
    public void test_rewrite_RealExecution() {
        // 构造真实入参
        Context context = new Context();
        Query rawQuery = new Query("2025年北京五一旅游天气合适吗？");
        context.setQuery(rawQuery);

        // 真正执行你写的 DMQR 查询改写逻辑
        List<Query> result = dmqrQueryRewrite.rewrite(context);

        // 验证结果
        System.out.println("生成改写查询数量：" + result.size());
        result.forEach(q -> System.out.println("→ " + q.getText()));

        assertFalse(result.isEmpty());
    }
}
