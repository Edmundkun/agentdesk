package com.agentdesk.knowledge.dto;

import com.agentdesk.knowledge.Document;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record DocumentVO(
        Long id,
        String name,
        String fileType,
        Long sizeBytes,
        Integer chunkCount,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static DocumentVO from(Document document) {
        return new DocumentVO(document.getId(), document.getName(), document.getFileType(),
                document.getSizeBytes(), document.getChunkCount(), document.getStatus(), document.getCreatedAt());
    }
}
