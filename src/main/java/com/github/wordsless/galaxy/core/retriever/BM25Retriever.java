package com.github.wordsless.galaxy.core.retriever;

import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.entity.Document;
import com.github.wordsless.galaxy.core.entity.Query;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BM25Retriever implements Retriever {
    private final VectorStore vectorStore;

    @Autowired
    public BM25Retriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<Document> retrieve(Query rewritedQuery) {
        AtomicLong sn = new AtomicLong(0L);
        var docs = new ArrayList<Document>();
        var springAiDocs = vectorStore.similaritySearch(rewritedQuery.getText());
        springAiDocs.forEach(doc -> {
            docs.add(new Document(sn.getAndIncrement(), 0L, doc.getText(), null));
        });
        return docs;
    }
}