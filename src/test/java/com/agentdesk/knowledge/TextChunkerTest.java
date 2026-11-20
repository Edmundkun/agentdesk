package com.agentdesk.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    @DisplayName("长文本按句子边界切片且不超过上限")
    void shouldSplitLongTextIntoBoundedChunks() {
        String text = "差旅报销需要在出差结束后十个工作日内提交。".repeat(60);

        List<String> chunks = TextChunker.split(text, 200, 40);

        assertThat(chunks).hasSizeGreaterThan(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(200));
    }

    @Test
    @DisplayName("相邻切片存在重叠，避免语义被截断")
    void shouldOverlapAdjacentChunks() {
        String text = "第一段内容用于测试切片重叠效果。".repeat(40);

        List<String> chunks = TextChunker.split(text, 120, 40);

        assertThat(chunks.size()).isGreaterThan(1);
        String tailOfFirst = chunks.get(0).substring(chunks.get(0).length() - 20);
        assertThat(chunks.get(1)).contains(tailOfFirst);
    }

    @Test
    @DisplayName("空文本返回空列表")
    void shouldReturnEmptyForBlankText() {
        assertThat(TextChunker.split("   \n\n ", 200, 40)).isEmpty();
        assertThat(TextChunker.split(null, 200, 40)).isEmpty();
    }

    @Test
    @DisplayName("超长句子被硬切分，不会产生超长切片")
    void shouldHardSplitVeryLongSentence() {
        String text = "啊".repeat(1000);

        List<String> chunks = TextChunker.split(text, 300, 50);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(300));
    }
}
