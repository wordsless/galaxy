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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class BertMicroServiceCaller {

    private final static Logger logger = LoggerFactory.getLogger(BertMicroServiceCaller.class);

    // BERTopic 微服务地址（改成你自己的）
    private String BERTOPIC_API = "http://127.0.0.1:8000/topic/predict";

    private String DPR_RETRIEVER_ROUTER = "http://127.0.0.1:8000/router";

    // Jackson（Spring 自带，spring-context 已包含）
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 BERTopic 微服务进行主题预测
     * @param query 待分析文本列表
     * @return 主题结果
     */
    public List<String> predictTopics(String query) {
        try {
            // 1. 构造请求 JSON
            Map<String, String> request = Map.of("texts", query);
            String jsonRequest = objectMapper.writeValueAsString(request);

            // 2. 调用 BERTopic 微服务（核心调用）
            String jsonResponse = HttpUtil.post(BERTOPIC_API, jsonRequest, 3, 500);

            // 3. 解析返回结果
            return objectMapper.readValue(jsonResponse, new TypeReference<List<String>>() {});
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public float[] queryEncode(final String type, final String query) {
        try {
            Map<String, String> request = Map.of("texts", query);
            String jsonRequest = objectMapper.writeValueAsString(request);
            String jsonResponse = HttpUtil.post(DPR_RETRIEVER_ROUTER+"/"+type, jsonRequest, 3, 500);
            return objectMapper.readValue(jsonResponse, new TypeReference<float[]>() {});
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
