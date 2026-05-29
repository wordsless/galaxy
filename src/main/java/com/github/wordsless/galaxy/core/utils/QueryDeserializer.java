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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wordsless.galaxy.core.entity.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QueryDeserializer extends StdDeserializer<Query> {

    public QueryDeserializer() {
        this(null);
    }

    public QueryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Query deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        ObjectNode node = parser.getCodec().readTree(parser);

        // 基础字段
        int sn = node.get("sn").asInt();
        String text = node.get("text").asText();

        // 反序列化 Entity 列表
        ArrayNode nersNode = (ArrayNode) node.get("NERs");
        List<Query.Entity> ners = new ArrayList<>();
        for (var entityNode : nersNode) {
            Query.Entity entity = ctx.readValue(entityNode.traverse(parser.getCodec()), Query.Entity.class);
            ners.add(entity);
        }

        // 反序列化候选主题
        ArrayNode topicsNode = (ArrayNode) node.get("candidateTopics");
        List<String> topics = new ArrayList<>();
        for (var topic : topicsNode) {
            topics.add(topic.asText());
        }

        return new Query(sn, text, ners, topics);
    }
}
