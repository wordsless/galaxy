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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import dev.langchain4j.model.chat.ChatModel;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.exception.NamedEntityRecognizeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基于远程ChatModel的命名实体识别器
 * 优化点：
 * 1. 构造函数参数校验
 * 2. 入参空值校验
 * 3. 异常上下文补充
 */
public class NamedEntityRecognizerWithChatModel
        extends ChatModelDelegator
        implements NamedEntityRecognizer {

    public NamedEntityRecognizerWithChatModel(
            ChatModel chatModel,
            String promptTemplate,
            Schema schema,
            ObjectMapper mapper) {
        // 严格校验入参，快速失败
        super(
                Objects.requireNonNull(chatModel, "ChatModel不能为空"),
                Objects.requireNonNull(promptTemplate, "提示词模板不能为空"),
                Objects.requireNonNull(schema, "Schema校验规则不能为空"),
                Objects.requireNonNull(mapper, "ObjectMapper不能为空")
        );
    }

    @Override
    public Map<String, String> recognize(String rawQuery) {
        // 校验原始查询
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("原始查询字符串不能为空或空白");
        }

        try {
            // 执行并传入原始查询（用于异常上下文）
            String json = super.execute(3, rawQuery, null);
            return mapper.readValue(json, HashMap.class);
        } catch (JsonMappingException e) {
            throw new NamedEntityRecognizeException(
                    "StructuredQuery反序列化失败，原始查询：" + rawQuery,
                    e, 3, super.promptTemplate);
        } catch (JsonProcessingException e) {
            throw new NamedEntityRecognizeException(
                    "JSON处理失败，原始查询：" + rawQuery,
                    e, 3, super.promptTemplate
            );
        }
    }
}