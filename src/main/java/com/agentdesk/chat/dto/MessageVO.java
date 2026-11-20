package com.agentdesk.chat.dto;

import com.agentdesk.agent.AgentStep;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record MessageVO(
        Long id,
        String role,
        String content,
        List<AgentStep> trace,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
