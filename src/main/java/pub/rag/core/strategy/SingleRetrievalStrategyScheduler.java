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

package pub.rag.core.strategy;

import lombok.NonNull;
import pub.rag.core.*;
import pub.rag.core.entity.Document;
import pub.rag.core.entity.PromptContext;
import pub.rag.core.response.RetrievalQualityResponse;
import pub.rag.core.exception.RetrievalStrategyScheduleException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SingleRetrievalStrategyScheduler<T extends RetrievalQualityResponse>
        extends AbstractRetrievalStrategyScheduler<T> {

    // 检索超时时间（秒）
    private static final long RETRIEVAL_TIMEOUT_SECONDS = 120L;
    // 最大重试次数
    private static final int MAXIMUM_RETRY_COUNT = 3;
    // 是否启用Schema校验
    private static final boolean SCHEMA_VALIDATABLE = true;

    /** 共享线程池：复用线程资源，避免频繁创建/销毁 */
    private final ExecutorService executorService;

    public SingleRetrievalStrategyScheduler(@NonNull final String template,
                                            @NonNull final ChatModelDelegator qualityEvaluator,
                                            @NonNull final Preprocessor preprocessor,
                                            @NonNull final Retriever retriever,
                                            @NonNull final Aligner aligner,
                                            @NonNull final Reranker reranker,
                                            final ExecutorService executorService) {
        super(template, qualityEvaluator, preprocessor,  retriever, aligner, reranker);
        if(executorService == null) {
            var ap = Runtime.getRuntime().availableProcessors();
            this.executorService = Executors.newFixedThreadPool(ap > 2 ? ap - 1 : 1);
        } else
            this.executorService = executorService;
    }

    public List<Document> retrieveWithoutPreprocessAndGenerate(String query) {
        List<Document> retrievedDocs = retriever.retrieve(query);
        // 过滤空文档，避免无效数据
        if (retrievedDocs == null || retrievedDocs.isEmpty()) {
            throw new RetrievalStrategyScheduleException("No relevant documents were retrieved.");
        }

        // 对齐 + 重排序
        List<Document> alignedDocuments = aligner.align(retrievedDocs);
        validateDocumentList(alignedDocuments, "alignment", query);

        List<Document> rerankedDocuments = reranker.rerank(alignedDocuments);
        validateDocumentList(rerankedDocuments, "reranking", query);

        return rerankedDocuments;
    }

    public T retrieve(String rawQuery) {
        // 前置校验：原始查询不能为空
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new RetrievalStrategyScheduleException("Original query must not be null or empty");
        }

        // 校验线程池状态
        validateExecutorService();

        List<Document> reranked = null;

        // Step 1: 预处理 - 重写原始查询
        List<String> rewritedQueries = preprocessor.process(rawQuery, null);
        if (rewritedQueries == null || rewritedQueries.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No rewritten queries after preprocessing, original query: [%s]", rawQuery));
        }
        // 过滤空的重写查询
        List<String> validRewritedQueries = rewritedQueries.stream()
                .filter(q -> q != null && !q.trim().isEmpty())
                .toList();
        if (validRewritedQueries.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("All rewritten queries are empty, original query: [%s]", rawQuery));
        }

        if(validRewritedQueries.size() == 1) {
            reranked = this.retrieveWithoutPreprocessAndGenerate(validRewritedQueries.getFirst());
        } else {
            // Step 2: 使用共享线程池进行初始批量检索
            List<Document> candidate = new CopyOnWriteArrayList<>();
            List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();

            for (String query : validRewritedQueries) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        var results = this.retrieveWithoutPreprocessAndGenerate(validRewritedQueries.getFirst());
                        candidate.addAll(results);
                    } catch (Exception e) {
                        throw new CompletionException(
                                String.format("Retrieval failed for query: [%s], error: %s", query, e.getMessage()), e);
                    }
                }, executorService);
                futures.add(future);
            }

            // 等待所有检索任务完成（带超时）
            waitForRetrievalTasks(futures, rawQuery);
            reranked = candidate;
        }
        // Step 5: 质量评估
        var qualityResponse = evaluateQuality(rawQuery, reranked);
        validateQualityResponse(qualityResponse, rawQuery);
        return qualityResponse;
    }

    @Override
    public String answer(String rawQuery) {
        var qualityResponse = retrieve(rawQuery);
        // 返回最终答案（确保答案非空）
        String finalAnswer = qualityResponse.getAnswer();
        if (finalAnswer == null || finalAnswer.trim().isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Quality evaluation returned empty answer, original query: [%s]", rawQuery));
        }
        return finalAnswer;
    }

    /**
     * 校验线程池状态
     * @throws RetrievalStrategyScheduleException 线程池已关闭/终止或为空时抛出异常
     */
    private void validateExecutorService() {
        if (executorService == null) {
            throw new RetrievalStrategyScheduleException("Executor service must not be null");
        }
        if (executorService.isShutdown() || executorService.isTerminated()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Executor service status invalid - shutdown: %s, terminated: %s",
                            executorService.isShutdown(), executorService.isTerminated()));
        }
    }

    /**
     * 等待所有检索任务完成，包含超时处理
     * @param futures 检索任务的Future列表
     * @param rawQuery 原始查询（用于错误信息）
     * @throws RetrievalStrategyScheduleException 任务中断、超时或执行失败时抛出异常
     */
    private void waitForRetrievalTasks(List<CompletableFuture<Void>> futures, String rawQuery) {
        if (futures.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No retrieval tasks to execute, original query: [%s]", rawQuery));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(RETRIEVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 重置线程中断状态
            throw new RetrievalStrategyScheduleException(
                    String.format("Retrieval tasks interrupted (query: [%s]), error: %s", rawQuery, e.getMessage()), e);
        } catch (TimeoutException e) {
            // 超时后取消所有未完成的任务
            futures.forEach(future -> future.cancel(true));
            throw new RetrievalStrategyScheduleException(
                    String.format("Retrieval tasks timed out after %ds (query: [%s])",
                            RETRIEVAL_TIMEOUT_SECONDS, rawQuery), e);
        } catch (ExecutionException e) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Retrieval tasks execution failed (query: [%s]), error: %s",
                            rawQuery, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                    e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * 验证文档列表非空且非null
     * @param documents 待验证的文档列表
     * @param stage 处理阶段（用于错误信息）
     * @param rawQuery 原始查询（用于错误信息）
     * @throws RetrievalStrategyScheduleException 文档列表无效时抛出异常
     */
    private void validateDocumentList(List<Document> documents, String stage, String rawQuery) {
        if (documents == null || documents.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No valid documents after %s process, original query: [%s]", stage, rawQuery));
        }
        // 额外校验：文档列表中是否有有效文档（非null）
        long validDocCount = documents.stream().filter(Objects::nonNull).count();
        if (validDocCount == 0) {
            throw new RetrievalStrategyScheduleException(
                    String.format("All documents are null after %s process, original query: [%s]", stage, rawQuery));
        }
    }

    /**
     * 验证质量评估响应有效性
     * @param qualityResponse 质量评估响应
     * @param rawQuery 原始查询（用于错误信息）
     * @throws RetrievalStrategyScheduleException 响应无效时抛出异常
     */
    private void validateQualityResponse(RetrievalQualityResponse qualityResponse, String rawQuery) {
        if (qualityResponse == null) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Quality evaluation returned null response, original query: [%s]", rawQuery));
        }
        if (qualityResponse.getQuality() < 0) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Invalid negative quality value: %d (query: [%s])",
                            qualityResponse.getQuality(), rawQuery));
        }
    }

    /**
     * 通用质量评估方法
     * @param rawQuery 原始查询
     * @param documents 待评估的文档列表
     * @return 质量评估响应
     */
    private T evaluateQuality(String rawQuery, List<Document> documents) {
        // 转换文档列表为字符串列表（过滤null文档）
        List<String> documentStrList = documents.stream()
                .filter(Objects::nonNull)
                .map(Document::toString)
                .filter(docStr -> docStr != null && !docStr.trim().isEmpty())
                .collect(Collectors.toList());

        if (documentStrList.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    String.format("No valid document strings for quality evaluation, original query: [%s]", rawQuery));
        }

        PromptContext promptContext = new PromptContext(
                template,
                rawQuery,
                null,
                null,
                documentStrList,
                null,
                null,
                null
        );

        // 调用质量评估器（带预定义参数）
        try {
            return (T) qualityEvaluator.invoke(MAXIMUM_RETRY_COUNT, SCHEMA_VALIDATABLE, promptContext, RetrievalQualityResponse.class);
        } catch (Exception e) {
            throw new RetrievalStrategyScheduleException(
                    String.format("Quality evaluation failed (query: [%s]), error: %s",
                            rawQuery, e.getMessage()), e);
        }
    }

    /**
     * 注意：线程池由外部注入，建议在使用完该调度器后，由外部负责关闭线程池
     * 示例：executorService.shutdown();
     */
    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}