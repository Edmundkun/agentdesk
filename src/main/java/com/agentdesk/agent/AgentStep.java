package com.agentdesk.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 单步推理轨迹，随消息一起持久化并在前端时间线中展示（可观测性）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {

    private int index;

    private String thought;

    private String tool;

    private String input;

    private String observation;

    private long latencyMs;
}
