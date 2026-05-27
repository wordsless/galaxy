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

package com.github.wordsless.galaxy.core.algorithm.star;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.entity.*;
import com.github.wordsless.galaxy.core.Retriever;
import com.github.wordsless.galaxy.core.algorithm.mcts.Action;
import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSNode;

import java.util.ArrayList;
import java.util.List;

public class StarAction implements Action<StarState> {

    private Retriever retriever;

    private ChatModelDelegator<List<Query>> generateSubqueryDelegator;

    private ChatModelDelegator<List<String>> generateAnswerDelegator;

    private ChatModelRequest generateSubqueryRequestTemplate;

    private ChatModelRequest generateAnswerRequest;

    public List<StarState> buildStateList(final MCTSNode<StarState> node) {
        var cur = node;
        var context = new ArrayList<StarState>();
        while(cur != null) {
            context.add(cur.getState());
            cur = cur.getParent();
        }
        return context;
    }

    public String buildContext(final MCTSNode<StarState> node) {
        var cur = node;
        var context = new StringBuilder();
        while(cur != null) {
            context.append(cur.toString());
            cur = node.getParent();
        }
        return context.toString();
    }


    @Override
    public String getLabel() {
        return "STAR";
    }

    @Override
    public MCTSNode<StarState> transit(MCTSNode<StarState> node, final Context context) {
        var request = generateSubqueryRequestTemplate.withRawQuery(context.getQuery())
                              .withContext(context);
        var subquery = generateSubqueryDelegator.delegate(request, new TypeReference<List<Query>>(){}).getFirst();
        var docs = retriever.retrieve(subquery);

        var pair = new EntityWithScore<Query, List<Document>>();
        pair.setEntity(subquery);
        pair.setValue(docs);

        context.getReferences().add(pair);

        request = this.generateAnswerRequest.withContext(context);
        var answer = this.generateAnswerDelegator.delegate(request, new TypeReference<List<String>>() {}).getFirst();

        var state = new StarState(context.getQuery().getText(), answer, false);
        return new MCTSNode<>(node, state);
    }
}
