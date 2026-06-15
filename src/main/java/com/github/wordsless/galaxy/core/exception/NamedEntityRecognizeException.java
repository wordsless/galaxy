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

package com.github.wordsless.galaxy.core.exception;

import lombok.Getter;

/**
 * 命名实体识别异常
 * 携带上下文信息，便于问题排查
 */
@Getter
public class NamedEntityRecognizeException extends RuntimeException {
    // 重试次数
    private final int retryCount;
    // 最终提示词
    private final String finalPrompt;

    public NamedEntityRecognizeException(final String message, final int retryCount, final String finalPrompt) {
        super(message);
        this.retryCount = retryCount;
        this.finalPrompt = finalPrompt;
    }

    public NamedEntityRecognizeException(String message, Throwable cause, int retryCount, String finalPrompt) {
        super(message, cause);
        this.retryCount = retryCount;
        this.finalPrompt = finalPrompt;
    }
}