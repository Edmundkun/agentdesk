package com.agentdesk.agent;

import java.util.List;

/**
 * Agent 执行结果：最终答案 + 完整推理轨迹 + 总耗时。
 */
public record AgentResult(String answer, List<AgentStep> steps, long durationMs) {
}
