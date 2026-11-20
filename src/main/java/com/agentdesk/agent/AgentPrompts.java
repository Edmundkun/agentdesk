package com.agentdesk.agent;

/**
 * Prompt 中的固定段落标记，Mock 模型与真实模型共用同一套协议。
 */
public final class AgentPrompts {

    public static final String QUESTION_HEADER = "【用户问题】";
    public static final String HISTORY_HEADER = "【历史对话】";
    public static final String SCRATCHPAD_HEADER = "【已有推理】";
    public static final String EMPTY = "（暂无）";

    private AgentPrompts() {
    }
}
