/*
 * MIT License
 *
 * Copyright (c) 2024 Qiang Li (李强)
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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import pub.rag.core.*;
import pub.rag.core.entity.Document;
import pub.rag.core.entity.PromptContext;
import pub.rag.core.exception.RetrievalStrategyScheduleException;
import pub.rag.core.response.RetrievalQualityResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Automatic Iteration Retrieval Strategy Scheduler
 * Supports automatic retry retrieval based on quality threshold until quality requirements are met or max retry times are reached
 * Type of quality evaluation response (must extend RetrievalQualityResponse)
 */
public class AutonomousRetrievalStrategyScheduler<T extends RetrievalQualityResponse>
        extends AbstractRetrievalStrategyScheduler<T> {

    private static final int MAXIMUM_RETRY_COUNT = 3;
    // 是否启用Schema校验
    private static final boolean SCHEMA_VALIDATABLE = true;
    // Constants for magic values
    private static final String DEFAULT_ANSWER = "No valid answer available";

    /** Quality threshold for retrieval results: stop retrying when this value is reached (integer type for consistency) */
    private final int qualityThreshold;
    /** Max retry times: return fallback result when exceeded */
    private final int maxRetryTimes;

    private final SingleRetrievalStrategyScheduler<T> singleRetrievalStrategy;

    private T bestAnswer;

    public AutonomousRetrievalStrategyScheduler(final int qualityThreshold,
                                                final int maxRetryTimes,
                                                @NonNull final String template,
                                                @NonNull final ChatModelDelegator qualityEvaluator,
                                                @NonNull final Preprocessor preprocessor,
                                                @NonNull final Retriever retriever,
                                                @NonNull final Aligner aligner,
                                                @NonNull final Reranker reranker) {
        super(template, qualityEvaluator, preprocessor,  retriever, aligner, reranker);
        this.qualityThreshold = qualityThreshold;
        this.maxRetryTimes = maxRetryTimes;
        this.singleRetrievalStrategy = new SingleRetrievalStrategyScheduler<T>(template, qualityEvaluator, preprocessor,  retriever, aligner, reranker, null);
    }

    /**
     * Execute retrieval and return final answer
     * @param rawQuery Original query string
     * @return Retrieved answer (fallback to DEFAULT_ANSWER if no valid result)
     * @throws RetrievalStrategyScheduleException If retrieval process fails
     */
    @Override
    public String answer(String rawQuery) {
        // Validate input and core components first
        validateInput(rawQuery);

        try {
            T qualityResponse = this.singleRetrievalStrategy.retrieve(rawQuery);

            // Return immediately if quality meets threshold
            if (qualityResponse.getConfidence() >= qualityThreshold) {
                return getValidAnswer((T) qualityResponse);
            }

            bestAnswer = qualityResponse;

            // Step 6: Automatic retry logic
            executeRetryLogic(rawQuery);

            // Step 7: Return best answer when retry times are exhausted
            return getValidAnswer(bestAnswer);

        } catch (RetrievalStrategyScheduleException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            throw new RetrievalStrategyScheduleException(
                    "Unexpected error during retrieval process, original query: " + rawQuery, e);
        }
    }

    /**
     * Validate input parameters
     * @param rawQuery Original query to validate
     * @throws RetrievalStrategyScheduleException If input is invalid
     */
    private void validateInput(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new RetrievalStrategyScheduleException("Original query must not be null or blank");
        }
        if (maxRetryTimes < 0) {
            throw new RetrievalStrategyScheduleException("Max retry times must be non-negative: " + maxRetryTimes);
        }
        if (qualityThreshold < 0) {
            throw new RetrievalStrategyScheduleException("Quality threshold must be non-negative: " + qualityThreshold);
        }
    }

    /**
     * Validate document list is not null or empty
     * @param documents Document list to validate
     * @param stage Process stage (for error message)
     * @param rawQuery Original query (for error message)
     * @throws RetrievalStrategyScheduleException If document list is invalid
     */
    private void validateDocumentList(List<Document> documents, String stage, String rawQuery) {
        if (documents == null || documents.isEmpty()) {
            throw new RetrievalStrategyScheduleException(
                    "No documents after " + stage + " process, original query: " + rawQuery);
        }
    }

    /**
     * Validate quality response is not null and has valid quality value
     * @param qualityResponse Quality evaluation response
     * @param rawQuery Original query (for error message)
     * @throws RetrievalStrategyScheduleException If quality response is invalid
     */
    private void validateQualityResponse(T qualityResponse, String rawQuery) {
        if (qualityResponse == null) {
            throw new RetrievalStrategyScheduleException(
                    "Quality evaluation returned null response, original query: " + rawQuery);
        }
        if (qualityResponse.getConfidence() < 0) {
            throw new RetrievalStrategyScheduleException(
                    "Invalid negative quality value: " + qualityResponse.getConfidence() + ", original query: " + rawQuery);
        }
    }

    /**
     * Execute retry logic until max retry times or quality threshold is reached
     * @param rawQuery Original query (for error message)
     */
    private void executeRetryLogic(String rawQuery) {
        int retryCount = 0;
        T currentQualityResponse = bestAnswer;

        while (retryCount < maxRetryTimes) {
            retryCount++;
            String nextQuery = currentQualityResponse.getNextTurnQueries().getFirst(); // only one

            // Validate next query
            if (nextQuery == null || nextQuery.isBlank()) {
                throw new RetrievalStrategyScheduleException(
                        "No next turn query for retry, retry count: " + retryCount + ", original query: " + rawQuery);
            }
            // Avoid duplicate retry with same query
            if (nextQuery.equals(rawQuery)) {
                throw new RetrievalStrategyScheduleException(
                        "Next turn query is same as original query (duplicate retry), retry count: " + retryCount + ", original query: " + rawQuery);
            }

            // Retry retrieval
            List<Document> retryRetrievedDocs = retriever.retrieve(nextQuery);
            validateDocumentList(retryRetrievedDocs, "retry retrieval", rawQuery);

            // Retry alignment + reranking
            List<Document> retryAlignedDocs = aligner.align(retryRetrievedDocs);
            validateDocumentList(retryAlignedDocs, "retry alignment", rawQuery);

            List<Document> retryRerankedDocs = reranker.rerank(retryAlignedDocs);
            validateDocumentList(retryRerankedDocs, "retry reranking", rawQuery);

            // Retry quality evaluation
            T retryQualityResponse = evaluateQuality(rawQuery, retryRerankedDocs);
            validateQualityResponse(retryQualityResponse, rawQuery);

            // Check if quality meets threshold
            if (retryQualityResponse.getConfidence() >= qualityThreshold) {
                bestAnswer = retryQualityResponse;
                return; // Exit retry loop immediately
            }

            // Update best answer if current retry result is better
            if (retryQualityResponse.getConfidence() > bestAnswer.getConfidence()) {
                bestAnswer = retryQualityResponse;
            }

            currentQualityResponse = retryQualityResponse;
        }
    }

    /**
     * Get valid answer from quality response (fallback to default if null/blank)
     * @param qualityResponse Quality evaluation response
     * @return Valid answer string
     */
    private String getValidAnswer(T qualityResponse) {
        return (qualityResponse.getAnswer() != null && !qualityResponse.getAnswer().isBlank())
                ? qualityResponse.getAnswer()
                : DEFAULT_ANSWER;
    }

    /**
     * Generic quality evaluation method
     * @param rawQuery Original query
     * @param documents Documents to evaluate
     * @return Quality evaluation response
     */
    private T evaluateQuality(String rawQuery, List<Document> documents) {
        // Convert document list to string list (stream with null check)
        List<String> documentStrList = documents.stream()
                .filter(Objects::nonNull)
                .map(Document::toString)
                .collect(Collectors.toList());

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

        // Invoke quality evaluator with predefined parameters
        return (T) chatModelDelegator.delegate(MAXIMUM_RETRY_COUNT, SCHEMA_VALIDATABLE, promptContext, new TypeReference<RetrievalQualityResponse>() {});
    }


    @Override
    public T retrieve(String query) {
        return null;
    }
}