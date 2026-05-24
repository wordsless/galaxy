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

package com.github.wordsless.galaxy.core.vectordb;

import com.github.wordsless.galaxy.core.entity.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

public class PgVectorDb implements VectorDatabase {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PgVectorDb(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void connect() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "vector vector(%d)," +
                        "metadata JSONB)",
                collectionName, dimension);
        jdbcTemplate.execute(sql);
    }

    @Override
    public void insert(String collectionName, List<VectorEntity> entities) {
        try {
            String sql = "INSERT INTO " + collectionName + "(vector, metadata) VALUES (?, ?::jsonb)";
            for (VectorEntity e : entities) {
                String vecStr = arrayToString(e.getVector());
                String metaJson = objectMapper.writeValueAsString(e.getMetadata());
                jdbcTemplate.update(sql, vecStr, metaJson);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ================================
    // 对齐 Milvus：返回自定义 SearchResult
    // ================================
    @Override
    public List<Document> search(String collectionName, float[] queryVector, int topK) {
        try {
            String q = arrayToString(queryVector);
            String sql = String.format(
                    "SELECT id, vector <-> '%s' AS distance " +
                            "FROM %s ORDER BY distance LIMIT %d",
                    q, collectionName, topK);

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Document result = new Document();
                result.setId(rs.getLong("id"));
                result.setDistance(rs.getFloat("distance"));
                return result;
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ================================
    // 根据 ID 取回完整文档
    // ================================
    @Override
    public VectorEntity getById(String collectionName, Long id) {
        try {
            String sql = "SELECT id, vector, metadata FROM " + collectionName + " WHERE id=?";
            return jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) -> {
                VectorEntity entity = new VectorEntity();
                entity.setId(rs.getLong("id"));
                entity.setVector(stringToArray(rs.getString("vector")));
                entity.setMetadata(rs.getObject("metadata", Map.class));
                return entity;
            });
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteById(String collectionName, Long id) {
        jdbcTemplate.update("DELETE FROM " + collectionName + " WHERE id=?", id);
    }

    @Override
    public void close() {
        // Spring JdbcTemplate 无需手动关闭
    }

    // ================================
    // double[] 工具方法
    // ================================
    private String arrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private float[] stringToArray(String str) {
        String[] parts = str.substring(1, str.length() - 1).split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Float.parseFloat(parts[i].trim());
        }
        return arr;
    }
}
