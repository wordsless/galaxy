package com.github.wordsless.galaxy.core.preprocessor;

import com.github.wordsless.galaxy.core.entity.Query;
import com.github.wordsless.galaxy.core.entity.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本地HanLP命名实体识别器 单元测试
 */
class NamedEntityRecognizerWithLocalTest {

    private NamedEntityRecognizerWithLocal nerLocal;
    private Context context;

    @BeforeEach
    void setUp() {
        nerLocal = new NamedEntityRecognizerWithLocal();
        // 模拟上下文和Query对象
        context = new Context();
    }

    /**
     * 测试：空上下文 抛出异常
     */
    @Test
    void process_WithContextNull_ThrowException() {
        assertThrows(IllegalArgumentException.class, () -> nerLocal.process(null));
    }

    /**
     * 测试：空Query 抛出异常
     */
    @Test
    void process_WithQueryNull_ThrowException() {
        when(context.getQuery()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> nerLocal.process(context));
    }

    /**
     * 测试：空文本/空白文本 抛出异常
     */
    @Test
    void process_WithTextBlank_ThrowException() {
        var query = new Query();
        query.setText("   ");
        context.setQuery(query);
        assertThrows(IllegalArgumentException.class, () -> nerLocal.process(context));
    }

    /**
     * 测试：正常文本 → 识别出人名、地名、机构名
     */
    @Test
    void process_WithNormalText_ExtractEntitiesSuccess() {
        // 测试文本：包含人名(张三)、地名(北京)、机构名(腾讯)
        String testText = "张三在北京腾讯公司上班";
        //when(query.getText()).thenReturn(testText);

        var query = new Query();
        query.setText(testText);
        context.setQuery(query);

        // 执行NER
        nerLocal.process(context);



        // 断言：实体列表不为空
        assertThat(query.getNERs()).isNotNull().isNotEmpty();
        List<Query.Entity> entities = query.getNERs();

        // 断言：包含PER/LOC/ORG三类实体
        assertThat(entities).anyMatch(e -> "PER".equals(e.getType()));
        assertThat(entities).anyMatch(e -> "LOC".equals(e.getType()));
        assertThat(entities).anyMatch(e -> "ORG".equals(e.getType()));

        // 断言：实体内容正确
        assertThat(entities).anyMatch(e -> "张三".equals(e.getText()));
        assertThat(entities).anyMatch(e -> "北京".equals(e.getText()));
        assertThat(entities).anyMatch(e -> "腾讯".equals(e.getText()));

        // 断言：位置信息有效
        assertThat(entities).allMatch(e -> e.getStart() >= 0 && e.getEnd() > e.getStart());
    }

    /**
     * 测试：无实体文本 → 返回空列表
     */
    @Test
    void process_WithNoEntityText_ReturnEmptyList() {
        var query  = new Query();
        query.setText("今天天气很好适合学习");
        context.setQuery(query);
        nerLocal.process(context);
        assertThat(query.getNERs()).isEmpty();
    }
}