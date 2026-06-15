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

import com.github.wordsless.galaxy.core.config.*;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        BasicConfig.class,
        ChatModelConfig.class,
        ChatModelDelegatorConfig.class,
        ChatModelRequestConfig.class,
        QueryRewriterConfig.class
})
@TestPropertySource(properties = {
        "decision.api.key=${DECISION_API_KEY}",
        "decision.base.url=https://api.deepseek.com",
        "decision.model.name=deepseek-v4-flash",
        "decision.model.temperature=0.7",
        "chat_model.request.retry=3"
})
public class ReFeQueryRewriteTest {

    @Autowired
    @Qualifier("RefineQueryRewriter")
    private RefineQueryRewriter refineQueryRewriter;

    private Context context;

    @BeforeEach
    public void setup() {
        context = new Context();
        context.setQuery(new Query("在《权力的游戏》原著中，曾担任过“国王之手”并最终死于自己策划的阴谋下的那位贵族，其家族纹章上的动物与哪一座被联合国教科文组织列为世界文化遗产的欧洲城堡的建造者家族纹章上的动物相同？\n（注：需要依次推理出人物 → 其家族 → 纹章动物 → 找到同样使用该动物的另一个家族 → 对应城堡及其建造者 → 确认该城堡是否为世界文化遗产。）"));
    }

    @Test
    public void testRefineQueryRewrite() {
        var results = refineQueryRewriter.rewrite(context);
        for(var item : results.entrySet()) {
            log.info(item.getKey() + ": " + item.getValue());
        }
        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty());
    }
}
