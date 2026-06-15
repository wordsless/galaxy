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

package com.github.wordsless.galaxy.core.reranker;

import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import com.github.wordsless.galaxy.core.Reranker;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.utils.MicroServiceCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Component
public class FiDLightReranker implements Reranker {

    private final int samplingCount;
    private final MicroServiceCaller<List<Double>> caller;
    private final Predictor<String, float[][]> encoder;

    // T5 配置
    private static final int K = 32;          // f_k 裁剪前32个向量
    private static final int HIDDEN_DIM = 768; // t5-base 维度

    @Autowired
    public FiDLightReranker(final int samplingTopK,
                            final int dimension,
                            final MicroServiceCaller<List<Double>> caller,
                            final Predictor<String, float[][]> encoder) {
        this.samplingCount = samplingTopK * dimension;
        this.caller = caller;
        this.encoder = encoder;
    }

    @Override
    public List<Document> rerank(List<Document> docs, final Context context) {
        // 1. 获取原始 query
        String query = context.getQuery().getText();
        if (docs == null || docs.isEmpty()) return new ArrayList<>();

        // 2. 对所有文档批量编码 + f_k 裁剪
        List<float[][]> compressedList = new ArrayList<>();
        for (Document doc : docs) {
            // 构造论文输入格式
            String inputText = "query: " + query + " content: " + doc.getContent();
            // 编码得到 [seq_len, 768]
            float[][] seq2d = null;
            try {
                seq2d = encoder.predict(inputText);
            } catch (TranslateException e) {
                throw new RuntimeException(e);
            }
            // f_k 裁剪：前 K 个向量
            float[][] compressed = Arrays.copyOf(seq2d, Math.min(K, seq2d.length));
            compressedList.add(compressed);
        }

        // 3. 拼接所有段落的向量 [total_len, 768]
        int totalLen = compressedList.size() * K;
        float[][] concat2d = new float[totalLen][HIDDEN_DIM];
        int idx = 0;
        for (float[][] arr : compressedList) {
            for (float[] vec : arr) {
                concat2d[idx++] = vec;
            }
        }

        // 4. 升维成 3D [1, totalLen, 768] (Decoder 必须格式)
        float[][][] encoderHidden3d = new float[1][][];
        encoderHidden3d[0] = concat2d;

        // 5. 展平 + GZIP 压缩
        float[] flat = flatten3d(encoderHidden3d);
        byte[] bytes = floatToByteArray(flat);
        byte[] gzipBytes = gzipCompress(bytes);

        // 6. 封装传输数据（shape + gzip base64）
        /*
        Map<String, Object> request = new HashMap<>();
        request.put("shape", new int[]{1, totalLen, HIDDEN_DIM});
        request.put("gzipData", Base64.getEncoder().encodeToString(gzipBytes));
        */
        var request = String.format("{\"shape\": [%d, %d, %d], \"data\": %s}", 1, totalLen, HIDDEN_DIM, Base64.getEncoder().encodeToString(gzipBytes));

        // 7. 调用 Decoder 微服务，返回文档索引列表 [1,3,5...]
        List<Double> indexDoubles = caller.call(request);
        List<Integer> indexes = new ArrayList<>();
        for (Double d : indexDoubles) indexes.add(d.intValue());

        // 8. 根据论文规则：按返回的 index 取文档（FiD-Light 指针机制）
        List<Document> result = new ArrayList<>();
        for (int idxDoc : indexes) {
            if (idxDoc >= 0 && idxDoc < docs.size()) {
                result.add(docs.get(idxDoc));
            }
        }
        return result;
    }

    // ========================== 工具方法 ==========================
    private float[] flatten3d(float[][][] array) {
        int batch = array.length;
        int seq = array[0].length;
        int dim = array[0][0].length;
        float[] flat = new float[batch * seq * dim];
        int i = 0;
        for (float[][] twoD : array) {
            for (float[] oneD : twoD) {
                System.arraycopy(oneD, 0, flat, i, oneD.length);
                i += oneD.length;
            }
        }
        return flat;
    }

    private byte[] floatToByteArray(float[] floats) {
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            bytes[i*4]   = (byte) (bits >> 24);
            bytes[i*4+1] = (byte) (bits >> 16);
            bytes[i*4+2] = (byte) (bits >> 8);
            bytes[i*4+3] = (byte) bits;
        }
        return bytes;
    }

    private byte[] gzipCompress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("GZIP 压缩失败", e);
        }
    }
}