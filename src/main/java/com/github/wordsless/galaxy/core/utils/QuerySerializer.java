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

package com.github.wordsless.galaxy.core.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.wordsless.galaxy.core.entity.Query;

import java.io.IOException;

public class QuerySerializer extends StdSerializer<Query> {

    public QuerySerializer() {
        this(null);
    }

    public QuerySerializer(Class<Query> t) {
        super(t);
    }

    @Override
    public void serialize(Query query, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("sn", query.getSn());
        gen.writeStringField("text", query.getText());

        // 序列化 NERs → 使用定制的 Entity 序列化器
        gen.writeFieldName("NERs");
        gen.writeStartArray();
        for (Query.Entity entity : query.getNERs()) {
            provider.defaultSerializeValue(entity, gen);
        }
        gen.writeEndArray();

        // 序列化候选主题
        gen.writeFieldName("candidateTopics");
        provider.defaultSerializeValue(query.getCandidateTopics(), gen);

        gen.writeEndObject();
    }
}
