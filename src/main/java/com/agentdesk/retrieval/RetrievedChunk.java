package com.agentdesk.retrieval;

/**
 * 检索命中的知识片段。
 */
public record RetrievedChunk(
        Long documentId,
        String documentName,
        Integer chunkIndex,
        String content,
        double score) {
}
