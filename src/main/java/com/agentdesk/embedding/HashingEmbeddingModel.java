package com.agentdesk.embedding;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地哈希向量模型（Hashing Trick + 符号哈希 + L2 归一化）。
 *
 * <p>不依赖任何外部 API 与模型文件，进程内毫秒级完成向量化，保证项目克隆下来就能跑通；
 * 通过 {@link EmbeddingModel} 接口可无缝替换为真实 Embedding 服务，检索与 Agent 层无需改动。</p>
 */
@Component
public class HashingEmbeddingModel implements EmbeddingModel {

    private static final int DIM = 384;
    private static final float BIGRAM_WEIGHT = 1.0f;
    private static final float UNIGRAM_WEIGHT = 0.6f;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIM];
        List<String> tokens = TextTokenizer.tokenize(text);
        for (String token : tokens) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, DIM);
            int sign = ((hash >>> 16) & 1) == 0 ? 1 : -1;
            float weight = token.length() > 1 ? BIGRAM_WEIGHT : UNIGRAM_WEIGHT;
            vector[index] += sign * weight;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += (double) v * v;
        }
        if (sum == 0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    @Override
    public int dimension() {
        return DIM;
    }

    @Override
    public String name() {
        return "hash-384";
    }
}
