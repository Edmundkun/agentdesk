package com.agentdesk.embedding;

/**
 * 文本向量化抽象：默认使用本地哈希向量模型，也可替换为 OpenAI / BGE 等远程 Embedding。
 */
public interface EmbeddingModel {

    float[] embed(String text);

    int dimension();

    String name();
}
