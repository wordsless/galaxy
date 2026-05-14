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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelRequest;
import com.github.wordsless.galaxy.core.ChatModelDelegator;

import java.util.Map;

/**
 * 基于远程ChatModel的命名实体识别器
 * 优化点：
 * 1. 构造函数参数校验
 * 2. 入参空值校验
 * 3. 异常上下文补充
 */
public class NamedEntityRecognizerWithChatModel implements NamedEntityRecognizer {

    private final ChatModelDelegator<Map<String, String>> chatModelDelegator;

    private final ChatModelRequest namedEntityRequest;

    public NamedEntityRecognizerWithChatModel(final ChatModelDelegator<Map<String, String>> chatModelDelegator,
                                              final ChatModelRequest namedEntityRequest) {
        this.chatModelDelegator = chatModelDelegator;
        this.namedEntityRequest = namedEntityRequest;
    }

    @Override
    public void process(Map<String, ?> context) {
        var rawQuery= (String) context.get("RawQuery");
        if(rawQuery == null || rawQuery.isEmpty())
            throw new NullPointerException("RawQuery is null");
        var request = namedEntityRequest.withRawQuery(rawQuery);
        var NERs = chatModelDelegator.delegate(request, new TypeReference<Map<String, String>>() {});
        ((Map) context).put("NERs", NERs);
    }
}