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

package com.github.wordsless.galaxy.core.strategy;

import com.github.wordsless.galaxy.core.*;
import pub.rag.core.*;

/**
 * Abstract base class for retrieval strategy schedulers
 * Defines core components required for retrieval process and standardizes retrieval execution flow
 * @param <T> Type of quality evaluation response
 */
public abstract class AbstractRetrievalStrategyScheduler<T> implements RetrievalStrategyScheduler, AutoCloseable {

    protected String template;
    protected ChatModelDelegator chatModelDelegator;
    protected Preprocessor preprocessor;
    protected Retriever retriever;
    protected Aligner aligner;
    protected Reranker reranker;

    public AbstractRetrievalStrategyScheduler(final String template,
                                              final ChatModelDelegator chatModelDelegator,
                                              final Preprocessor preprocessor,
                                              final Retriever retriever,
                                              final Aligner aligner,
                                              final Reranker reranker) {
        this.template = template;
        this.chatModelDelegator = chatModelDelegator;
        this.preprocessor = preprocessor;
        this.retriever = retriever;
        this.aligner = aligner;
        this.reranker = reranker;
    }

    public abstract T retrieve(String query);

    /**
     * Close resources (empty implementation for base class, to be overridden by subclasses if needed)
     * @throws Exception If resource closing fails
     */
    @Override
    public void close() throws Exception {
        // Default empty implementation - subclasses can override to close resources like thread pools
    }
}