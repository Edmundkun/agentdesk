package com.agentdesk.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "检索内容不能为空") String query,
        @Min(value = 1, message = "topK 最小为 1")
        @Max(value = 10, message = "topK 最大为 10")
        Integer topK) {

    public int topKOrDefault(int fallback) {
        return topK == null ? fallback : topK;
    }
}
