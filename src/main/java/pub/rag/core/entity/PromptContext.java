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

package pub.rag.core.entity;

import lombok.*;


import java.util.List;
import java.util.Map;

/**
 * Context container that holds all necessary data for constructing an AI prompt.
 * Encapsulates user query, context fragments, conversation history, and error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@With
public class PromptContext {

    /**
     * Present prompt template
     */
    private String template;

    /**
     * User's input query or question (Required)
     */
    private String rawQuery;

    /**
     * NER Entity List
     */
    private Map<String, String> entities;

    /**
     * Rewrited queries
     */
    private List<String> rewritedQueries;

    /**
     * Retrieved document fragments or context snippets (Optional)
     */
    private List<String> fragments;

    /**
     * Historical conversation records between user and assistant (Optional)
     */
    private List<Conversation> conversations;

    /**
     * Error messages from previous execution or validation (Optional)
     */
    private List<String> errors;

    /**
     * Extended field for custom business parameters (Optional)
     */
    private Object extra;
}