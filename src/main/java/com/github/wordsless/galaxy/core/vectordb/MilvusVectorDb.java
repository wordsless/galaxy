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
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResultData;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.*;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class MilvusVectorDb implements VectorDatabase {

    private final MilvusServiceClient milvusClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MilvusVectorDb(String host, int port) {
        this.milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );
    }

    @Override
    public void connect() {}

    @Override
    public void createCollection(String collectionName, int dimension) {
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(DataType.JSON)
                .build();

        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(metadataField)
                .build();

        milvusClient.createCollection(param);
    }

    @Override
    public void insert(String collectionName, List<VectorEntity> entities) {
        try {
            List<List<Float>> vectors = new ArrayList<>();
            List<String> metadataList = new ArrayList<>();

            for (VectorEntity e : entities) {
                List<Float> vec = new ArrayList<>();
                for (double d : e.getVector()) vec.add((float) d);
                vectors.add(vec);
                metadataList.add(objectMapper.writeValueAsString(e.getMetadata()));
            }

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("vector", vectors));
            fields.add(new InsertParam.Field("metadata", metadataList));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
        } catch (Exception ignored) {}
    }

    // ======================================================
    // 【关键修复】返回自定义 SearchResult，内部使用 SearchResultData
    // ======================================================
    @Override
    public List<Document> search(String collectionName, float[] queryVector, int topK) {
        try {
            List<Float> queryList = new ArrayList<>();
            for (double d : queryVector)
                queryList.add((float) d);

            SearchParam param = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withTopK(topK)
                    .withVectors(queryList)
                    .withVectorFieldName("vector")
                    .build();

            // 1. 获取 Milvus 原生 SearchResultData
            SearchResultData searchResultData = milvusClient.search(param).getData().getResults();

            // 2. 包装结果
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResultData);

            // 3. 转换为你项目的自定义 SearchResult
            List<Document> results = new ArrayList<>();
            for (SearchResultsWrapper.IDScore score : wrapper.getIDScore(0)) {
                Document res = new Document();
                res.setId(score.getLongID());
                res.setDistance(score.getScore());
                results.add(res);
            }

            return results;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public VectorEntity getById(String collectionName, Long id) {
        try {
            QueryParam param = QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr("id == " + id)
                    .withOutFields(List.of("vector", "metadata"))
                    .build();

            QueryResultsWrapper wrapper = new QueryResultsWrapper(milvusClient.query(param).getData());
            List<QueryResultsWrapper.RowRecord> records = wrapper.getRowRecords();

            if (records.isEmpty())
                return null;

            QueryResultsWrapper.RowRecord record = records.get(0);
            List<Float> vecList = (List<Float>) record.get("vector");
            var vector = new float[vecList.size()];
            for (int i = 0; i < vecList.size(); i++) {
                vector[i] = vecList.get(i);
            }
            String metaJson = (String) record.get("metadata");

            Map<String, Object> metadata = objectMapper.readValue(metaJson, Map.class);
            return new VectorEntity(id, vector, metadata);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteById(String collectionName, Long id) {
        DeleteParam param = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr("id == " + id)
                .build();
        milvusClient.delete(param);
    }

    @Override
    public void close() {
        milvusClient.close();
    }
}

