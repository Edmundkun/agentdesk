package com.agentdesk.agent;

/**
 * Agent 可调用的工具：新增能力只需实现该接口并注册为 Spring Bean。
 */
public interface Tool {

    /**
     * 工具名，需与 Prompt 中给出的名称完全一致。
     */
    String name();

    /**
     * 给模型看的工具说明（能力边界 + 入参格式）。
     */
    String description();

    /**
     * 执行工具。抛出的异常会被 AgentExecutor 捕获并作为 Observation 回填给模型。
     */
    String execute(String input, AgentContext context) throws Exception;
}
