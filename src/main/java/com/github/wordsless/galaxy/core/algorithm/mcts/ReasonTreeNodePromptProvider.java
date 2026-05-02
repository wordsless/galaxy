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

package com.github.wordsless.galaxy.core.algorithm.mcts;

import lombok.Builder;
import com.github.wordsless.galaxy.core.PromptProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Builder
public class ReasonTreeNodePromptProvider extends PromptProvider {

    private final Map<String, String> prompts = new HashMap<>();

    public final String buildSystemAnalysisPrompt(final String query) {
        var template = prompts.get("SAY");
        if(template == null) {
            try {
                template = super.readFromClasspath("AirRAG/SAY.md");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return template.replace("{query}", query);
    }

    public final String buildDirectAnswerPrompt(final String query) {
        var template = prompts.get("SAY");
        if(template == null) {
            try {
                template = super.readFromClasspath("AirRAG/DA.md");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return template.replace("{query}", query);
    }

    public static final String buildRetrievalAnswer() {
        return null;
    }

    public static final String buildQueryTransformation() {
        return null;
    }

    public static final String buildSummaryAnswer() {
        return null;
    }
}
