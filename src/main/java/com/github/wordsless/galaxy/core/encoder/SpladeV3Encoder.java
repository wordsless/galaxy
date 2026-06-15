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
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Component
public class SpladeV3Encoder {

    private static final String MODEL = "naver/splade-v3";

    @Value("splade.v3.threshold")
    private float THRESHOLD = 0.01f;

    private HuggingFaceTokenizer tokenizer;
    private ZooModel<String[], float[][]> model;
    private Predictor<String[], float[][]> predictor;

    @PostConstruct
    public void init() throws Exception {
        tokenizer = HuggingFaceTokenizer.newInstance(MODEL);
        Criteria<String[], float[][]> criteria = Criteria.builder()
                .setTypes(String[].class, float[][].class)
                .optModelUrls(MODEL)
                .build();
        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    /**
     * 生成 SPLADE v3 稀疏向量：token → weight
     */
    public Map<String, Float> encode(String text) {
        Map<String, Float> sparse = new HashMap<>();
        try {
            if (text.length() > 512) text = text.substring(0, 512);
            Encoding enc = tokenizer.encode(text);
            float[][] out = predictor.predict(new String[]{text});
            float[] logits = out[0];

            for (int i = 0; i < logits.length; i++) {
                float w = Math.max(0.0f, logits[i]);
                if (w > THRESHOLD) {
                    String token = enc.getTokens()[i];
                    sparse.put(token, sparse.getOrDefault(token, 0f) + w);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("SPLADE v3 推理失败", e);
        }
        return sparse;
    }

    @PreDestroy
    public void close() {
        if (predictor != null)
            predictor.close();
        if (model != null)
            model.close();
    }
}
