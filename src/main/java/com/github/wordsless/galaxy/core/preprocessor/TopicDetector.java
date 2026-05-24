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

package com.github.wordsless.galaxy.core.preprocessor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.ChatModelRequest;
import com.github.wordsless.galaxy.core.entity.Context;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TopicDetector implements IQueryFilter {

    private ChatModelRequest topicModelRequest;

    private ChatModelDelegator<List<String>> topicModelDelegator;

    @Override
    public void process(final Context context) {
        var query   = context.getQuery();
        var request = topicModelRequest.withRawQuery(query);
        var topics  = topicModelDelegator.delegate(request, new TypeReference<List<String>>() {});
        query.getCandidateTopics().addAll(topics);
        topics = query.getCandidateTopics();
        List<String> uniqueList = topics.stream().distinct().toList();
        query.setCandidateTopics(uniqueList);
    }
}
