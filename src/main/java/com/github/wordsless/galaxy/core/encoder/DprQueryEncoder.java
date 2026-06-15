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

package com.github.wordsless.galaxy.core.encoder;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.github.wordsless.galaxy.core.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Slf4j
public class DprQueryEncoder implements Encoder<float[]> {

    // 默认使用Facebook的DPR查询编码器模型
    private static final String DEFAULT_MODEL = "facebook/dpr-question_encoder-single-nq-base";
    // DPR输出的稠密向量维度（nq-base版本为768维）
    private static final int DEFAULT_VECTOR_DIM = 768;

    @Value("dpr.query.model")
    private String modelName = DEFAULT_MODEL;

    @Value("dpr.query.max.length")
    private int maxLength = 64;

    @Value("dpr.query.normalize")
    private boolean normalizeVector = true;

    private HuggingFaceTokenizer tokenizer;
    private ZooModel<String[], NDArray> model;
    private Predictor<String[], NDArray> predictor;
    private NDManager ndManager;

    @PostConstruct
    public void init() throws Exception {
        // 初始化NDManager用于数组管理
        ndManager = NDManager.newBaseManager();
        // 加载DPR查询编码器对应的tokenizer
        tokenizer = HuggingFaceTokenizer.newInstance(modelName);
        // 构建模型加载条件
        Criteria<String[], NDArray> criteria = Criteria.builder()
                .setTypes(String[].class, NDArray.class)
                .optModelUrls(modelName)
                .build();
        // 加载模型和预测器
        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    /**
     * 生成DPR查询稠密向量
     * @param query 输入查询文本
     * @return 归一化后的稠密向量（float数组）
     */
    @Override
    public float[] encode(String query) {
        try {
            // 文本长度截断
            if (query.length() > maxLength) {
                query = query.substring(0, maxLength);
            }

            // 文本编码（tokenize）
            Encoding encoding = tokenizer.encode(query);
            // 模型推理获取稠密向量
            NDArray output = predictor.predict(new String[]{query});

            // 取第一个维度（batch维度）的向量，转换为float数组
            float[] vector = output.get(0).toFloatArray();

            // 向量归一化（可选）
            if (normalizeVector) {
                vector = normalize(vector);
            }

            return vector;
        } catch (Exception e) {
            throw new RuntimeException("DPR Query Encoder 推理失败", e);
        }
    }

    /**
     * 对向量进行L2归一化
     * @param vector 原始向量
     * @return 归一化后的向量
     */
    private float[] normalize(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        // 防止除零
        if (norm < 1e-6) {
            return vector;
        }
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    @PreDestroy
    public void close() {
        // 资源释放
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
        if (ndManager != null) {
            ndManager.close();
        }
    }

    // 可选：获取向量维度
    public int getVectorDimension() {
        return DEFAULT_VECTOR_DIM;
    }
}
