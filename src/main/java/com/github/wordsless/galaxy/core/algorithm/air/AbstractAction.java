/*
 * MIT License
 * Copyright (c) ${YEAR} Qiang Li (李强)
 */
package com.github.wordsless.galaxy.core.algorithm.air;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.wordsless.galaxy.core.ChatModelDelegator;
import com.github.wordsless.galaxy.core.ChatModelRequest;
import com.github.wordsless.galaxy.core.algorithm.mcts.Action;
import com.github.wordsless.galaxy.core.algorithm.mcts.MCTSNode;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractAction<S extends ReasoningState<?>> implements Action<S> {

    protected final ChatModelRequest template;
    // 统一管理魔法值
    protected static final String EMPTY_CONTEXT = "[]";

    public AbstractAction(@NotNull final ChatModelRequest template) {
        this.template = Objects.requireNonNull(template, "Template cannot be null");
    }

    // 抽取通用的状态构建逻辑
    public List<ReasoningState<?>> buildState(MCTSNode<?> node) {
        Objects.requireNonNull(node, "Node cannot be null");
        var cur = node;
        var states = new ArrayList<ReasoningState<?>>();
        while (cur != null) {
            states.add((ReasoningState<?>) cur.getState());
            cur = cur.getParent(); // 修复原代码bug：cur = node.getParent() 会导致死循环
        }
        return states;
    }

    // 抽取通用的上下文拼接逻辑
    protected String buildContextString(List<ReasoningState<?>> history) {
        if (history.isEmpty()) {
            return EMPTY_CONTEXT;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i).toString());
            if (i < history.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    // 泛型化的核心transit逻辑模板方法
    @SuppressWarnings("unchecked")
    protected MCTSNode<S> doTransit(
            @NonNull MCTSNode<S> node,
            @NonNull ChatModelDelegator<S> delegator,
            @NonNull TypeReference<S> typeReference,
            String actionLabel
    ) {
        // 统一空指针防护
        Objects.requireNonNull(node, "Node cannot be null");
        Objects.requireNonNull(delegator, "Delegator cannot be null");
        Objects.requireNonNull(typeReference, "TypeReference cannot be null");

        // 构建上下文
        List<ReasoningState<?>> history = buildState(node);
        String context = buildContextString(history);

        // 构建请求并调用delegator
        ChatModelRequest request = template.withContext(context);
        S state = delegator.delegate(request, typeReference);

        // 设置状态属性
        state.setAction(Objects.requireNonNullElse(actionLabel, this.getLabel()));
        state.setFinal(true);

        // 返回新节点
        return new MCTSNode<S>(node, state);
    }

    // 子类需实现：提供TypeReference（简化泛型传递）
    protected abstract TypeReference<S> getTypeReference();

    // 子类需实现：提供Delegator（统一命名）
    protected abstract ChatModelDelegator<S> getDelegator();

    // 统一实现transit方法，子类无需重复编写
    @Override
    public MCTSNode<S> transit(MCTSNode<S> node) {
        var state = node.getState();
        var planStep = state.getPlanStep();
        // if planStep is null, means it is final node, then return null.
        // It should not reach here.
        if(planStep == null) {
            return null;
        }
        // if planStep.next is null, means it is final node, then return null.
        var nextPlanStep = planStep.next();
        if(nextPlanStep == null) {
            return null;
        }
        var child = doTransit(
                node,
                getDelegator(),
                getTypeReference(),
                getLabel()
        );
        nextPlanStep.setUsed(true);
        child.getState().setPlanStep(nextPlanStep);
        return child;
    }
}