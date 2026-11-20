package com.agentdesk.agent;

/**
 * 构造 Prompt 用的历史消息。
 */
public record HistoryMessage(String role, String content) {
}
