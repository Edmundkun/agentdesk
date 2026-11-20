package com.agentdesk.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorUtilsTest {

    @Test
    @DisplayName("向量序列化与反序列化保持一致")
    void shouldRoundTripVector() {
        float[] vector = {0.1234f, -0.5f, 0.98765f};

        float[] restored = VectorUtils.deserialize(VectorUtils.serialize(vector));

        assertThat(restored).hasSize(3);
        assertThat(restored[0]).isCloseTo(0.1234f, within(0.0002f));
        assertThat(restored[2]).isCloseTo(0.98765f, within(0.0002f));
    }

    @Test
    @DisplayName("正交向量余弦为 0，反向向量余弦为 -1")
    void shouldComputeCosine() {
        assertThat(VectorUtils.cosine(new float[]{1, 0}, new float[]{0, 1})).isZero();
        assertThat(VectorUtils.cosine(new float[]{1, 0}, new float[]{-1, 0})).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    @DisplayName("空向量或维度不一致时返回 0，避免除零")
    void shouldReturnZeroForInvalidInput() {
        assertThat(VectorUtils.cosine(new float[0], new float[0])).isZero();
        assertThat(VectorUtils.cosine(new float[]{1}, new float[]{1, 2})).isZero();
        assertThat(VectorUtils.cosine(null, new float[]{1})).isZero();
    }
}
