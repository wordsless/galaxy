package com.github.wordsless.galaxy.core.preprocessor;

import com.github.wordsless.galaxy.core.config.BasicConfig;
import com.github.wordsless.galaxy.core.config.ChatModelRequestAndDelegatorConfig;
import com.github.wordsless.galaxy.core.config.NamedEntityRecognizeConfig;
import com.github.wordsless.galaxy.core.exception.NamedEntityRecognizeException;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 👇 只保留 Spring，绝对不能加 MockitoExtension
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        BasicConfig.class,
        ChatModelRequestAndDelegatorConfig.class,
        NamedEntityRecognizeConfig.class
})
@TestPropertySource(properties = {
        "decision.api.key=sk-e7fee9f16f0c4dea97c0a949a62403b9",
        "decision.base.url=https://api.deepseek.com",
        "decision.model.name=deepseek-v4-pro",
        "decision.model.temperature=0.7",
        "chat_model.request.retry=3"
})
public class NamedEntityRecognizerWithChatModelTest {

    // ====================== 关键修改 ======================
    // 用 @MockitoBean 代替 @Mock
    // 作用：创建 mock，并自动替换 Spring 里的同名 Bean
    // ======================================================

    // 这个现在一定不会是 null
    @Autowired
    @Qualifier("NamedEntityRecognizerWithChatModel")
    private NamedEntityRecognizer namedEntityRecognizerWithChatModel;

    private Context context;
    private Query query;

    @BeforeEach
    void setUp() {
        query = new Query();
        context = new Context();
        context.setQuery(query);
    }

    // ====================== 你的测试逻辑完全不用改 ======================

    @Test
    void process_withNormalText_shouldReturnEntities() {
        query.setText("张三在上海腾讯工作");
        Query.Entity entity1 = new Query.Entity("张三", "PER", 0, 2);
        Query.Entity entity2 = new Query.Entity("上海", "LOC", 3, 5);
        Query.Entity entity3 = new Query.Entity("腾讯", "ORG", 5, 7);
        List<Query.Entity> mockResult = List.of(entity1, entity2, entity3);
        // 执行
        namedEntityRecognizerWithChatModel.process(context);

        // 验证
        assertThat(query.getNERs()).isNotEmpty().hasSize(3);
    }

    @Test
    void process_withEmptyText_shouldReturnEmptyList() {
        query.setText("");
        namedEntityRecognizerWithChatModel.process(context);

        assertThat(query.getNERs()).isEmpty();
    }

    @Test
    void process_whenModelFails_shouldThrowNamedEntityRecognizeException() {
        query.setText("张三");
        assertThatThrownBy(() -> namedEntityRecognizerWithChatModel.process(context))
                .isInstanceOf(NamedEntityRecognizeException.class)
                .hasMessageContaining("命名实体识别失败");
    }
}