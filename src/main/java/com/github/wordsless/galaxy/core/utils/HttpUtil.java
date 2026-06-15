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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HttpUtil {
    // 日志记录
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    // 默认重试次数
    private static final int DEFAULT_RETRY_COUNT = 3;
    // 默认重试间隔（毫秒）
    private static final long DEFAULT_RETRY_INTERVAL_MS = 1000;

    // 保留原有GET方法签名，使用默认重试配置
    public static String get(String apiUrl) throws Exception {
        return get(apiUrl, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL_MS);
    }

    // 带重试机制的GET方法
    public static String get(String apiUrl, int retryCount, long retryIntervalMs) throws Exception {
        // 封装重试逻辑
        return retryOnFailure(() -> {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // 设置连接超时和读取超时
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 检查响应码
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("GET request failed, response code: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        }, retryCount, retryIntervalMs, "GET", apiUrl);
    }

    // 保留原有POST方法签名，使用默认重试配置
    public static String post(String apiUrl, String jsonBody) throws IOException {
        try {
            return post(apiUrl, jsonBody, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL_MS);
        } catch (Exception e) {
            // 兼容原有方法的异常类型
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        }
    }

    // 带重试机制的POST方法
    public static String post(String apiUrl, String jsonBody, int retryCount, long retryIntervalMs) throws Exception {
        // 封装重试逻辑
        return retryOnFailure(() -> {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                // 设置连接超时和读取超时
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 检查响应码
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("POST request failed, response code: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        }, retryCount, retryIntervalMs, "POST", apiUrl);
    }

    /**
     * 通用重试逻辑封装
     * @param task 要执行的HTTP请求任务
     * @param retryCount 重试次数
     * @param retryIntervalMs 重试间隔（毫秒）
     * @param method HTTP方法（GET/POST）
     * @param apiUrl 接口地址
     * @return 接口响应结果
     * @throws Exception 最终失败时抛出异常
     */
    private static String retryOnFailure(HttpTask task,
                                         int retryCount,
                                         long retryIntervalMs,
                                         String method,
                                         String apiUrl) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= retryCount) {
            try {
                attempt++;
                logger.info("{} request to {} (attempt {}/{})", method, apiUrl, attempt, retryCount + 1);
                // 执行实际的HTTP请求
                return task.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("{} request to {} failed (attempt {}): {}", method, apiUrl, attempt, e.getMessage());

                // 如果是最后一次重试，直接抛出异常
                if (attempt >= retryCount + 1) {
                    break;
                }

                // 重试间隔
                try {
                    TimeUnit.MILLISECONDS.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry sleep interrupted", ie);
                    throw ie;
                }
            }
        }

        logger.error("{} request to {} failed after {} retries", method, apiUrl, retryCount);
        throw lastException;
    }

    /**
     * 函数式接口，封装HTTP请求任务
     */
    @FunctionalInterface
    private interface HttpTask {
        String execute() throws IOException;
    }
}