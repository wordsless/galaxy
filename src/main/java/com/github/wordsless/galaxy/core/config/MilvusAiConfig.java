package com.github.wordsless.galaxy.core.config;

import com.github.wordsless.galaxy.core.vectordb.MilvusVectorDB;
import com.github.wordsless.galaxy.core.vectordb.VectorDB;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MilvusAiConfig {

    // 全局单例MilvusServiceClient，Spring全权管控生命周期
    @Bean(destroyMethod = "close") // 容器停机自动执行client.close(3)
    public MilvusServiceClient milvusServiceClient(){
        ConnectParam param = ConnectParam.newBuilder()
                .withUri("http://127.0.0.1:19530")
                .withConnectTimeout(5000, TimeUnit.MILLISECONDS)
                .build();
        return new MilvusServiceClient(param);
    }

    // 注入client构造MilvusVectorDB，和你之前安全写法匹配
    @Bean
    public VectorDB milvusVectorDB(MilvusServiceClient client, EmbeddingModel model){
        return new MilvusVectorDB(client,"test_collection", model.dimension(), model);
    }
}