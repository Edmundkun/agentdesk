package com.agentdesk.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashingEmbeddingModelTest {

    private final HashingEmbeddingModel model = new HashingEmbeddingModel();

    @Test
    @DisplayName("向量维度固定且已 L2 归一化")
    void shouldProduceNormalizedVector() {
        float[] vector = model.embed("差旅报销需要在十个工作日内提交");

        assertThat(vector).hasSize(384);
        double norm = 0;
        for (float value : vector) {
            norm += (double) value * value;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    @DisplayName("相同文本余弦相似度为 1")
    void shouldGiveSameVectorForSameText() {
        float[] a = model.embed("发票抬头必须是公司全称");
        float[] b = model.embed("发票抬头必须是公司全称");

        assertThat(VectorUtils.cosine(a, b)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    @DisplayName("相关文本的相似度高于无关文本")
    void shouldRankRelatedTextHigher() {
        float[] query = model.embed("差旅报销提交时限是多久");
        float[] related = model.embed("差旅报销需在出差结束后十个工作日内提交，逾期不再受理。");
        float[] unrelated = model.embed("今天天气晴朗，适合去公园散步。");

        assertThat(VectorUtils.cosine(query, related)).isGreaterThan(VectorUtils.cosine(query, unrelated));
    }

    @Test
    @DisplayName("空文本返回零向量而不是抛异常")
    void shouldHandleBlankText() {
        float[] vector = model.embed("   ");

        assertThat(vector).hasSize(384);
        assertThat(VectorUtils.serialize(vector)).isNotBlank();
    }
}
