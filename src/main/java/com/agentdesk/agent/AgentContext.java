package com.agentdesk.agent;

/**
 * 一次 Agent 调用的上下文（用户隔离，工具只能访问当前用户的数据）。
 */
public record AgentContext(Long userId, Long conversationId) {
}
