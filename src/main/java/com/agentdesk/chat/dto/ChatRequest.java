package com.agentdesk.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        Long conversationId,
        @NotBlank(message = "问题不能为空")
        @Size(max = 2000, message = "问题最长 2000 字")
        String question) {
}
