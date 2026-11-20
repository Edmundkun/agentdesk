package com.agentdesk.llm;

/**
 * 大模型抽象层：屏蔽具体厂商差异，Agent 只依赖该接口。
 */
public interface ChatModel {

    /**
     * 输入完整 Prompt，返回模型原始输出（ReAct 格式）。
     */
    String chat(String prompt);

    String name();
}
