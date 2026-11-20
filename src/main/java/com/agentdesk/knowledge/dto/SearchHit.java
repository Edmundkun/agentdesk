package com.agentdesk.knowledge.dto;

public record SearchHit(
        Long documentId,
        String documentName,
        Integer chunkIndex,
        String content,
        double score) {
}
