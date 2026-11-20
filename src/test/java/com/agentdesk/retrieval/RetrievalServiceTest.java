package com.agentdesk.retrieval;

import com.agentdesk.config.AppProperties;
import com.agentdesk.embedding.HashingEmbeddingModel;
import com.agentdesk.embedding.VectorUtils;
import com.agentdesk.knowledge.Document;
import com.agentdesk.knowledge.DocumentChunk;
import com.agentdesk.knowledge.DocumentChunkMapper;
import com.agentdesk.knowledge.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkMapper chunkMapper;

    @Mock
    private DocumentMapper documentMapper;

    private final HashingEmbeddingModel embeddingModel = new HashingEmbeddingModel();

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getRag().setTopK(4);
        properties.getRag().setMinScore(0.01);
        properties.getRag().setKeywordWeight(0.3);
        retrievalService = new RetrievalService(chunkMapper, documentMapper, embeddingModel, properties);
    }

    @Test
    @DisplayName("混合检索把最相关片段排在第一位")
    void shouldRankRelevantChunkFirst() {
        when(chunkMapper.selectList(any())).thenReturn(List.of(
                chunk(1L, 10L, 0, "差旅报销需在出差结束后十个工作日内提交，超过三十个工作日不再受理。"),
                chunk(2L, 11L, 0, "新员工入职当天需携带身份证原件、学历证书与离职证明。")));
        when(documentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(10L, "员工报销制度.md"),
                document(11L, "新员工入职指南.md")));

        List<RetrievedChunk> hits = retrievalService.search(1L, "差旅报销提交时限", 4);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).documentName()).isEqualTo("员工报销制度.md");
        assertThat(hits.get(0).score()).isGreaterThan(0.1);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.score()).isBetween(0d, 1d));
    }

    @Test
    @DisplayName("没有切片时不返回结果，也不查询文档表")
    void shouldReturnEmptyWhenNoChunks() {
        when(chunkMapper.selectList(any())).thenReturn(List.of());

        assertThat(retrievalService.search(1L, "报销", 4)).isEmpty();
        verifyNoInteractions(documentMapper);
    }

    @Test
    @DisplayName("topK 限制返回条数")
    void shouldLimitTopK() {
        when(chunkMapper.selectList(any())).thenReturn(List.of(
                chunk(1L, 10L, 0, "差旅报销标准：一线城市每晚不超过六百元。"),
                chunk(2L, 10L, 1, "差旅报销需在十个工作日内提交。"),
                chunk(3L, 10L, 2, "报销发票抬头必须是公司全称。")));
        when(documentMapper.selectBatchIds(any())).thenReturn(List.of(document(10L, "员工报销制度.md")));

        assertThat(retrievalService.search(1L, "差旅报销", 1)).hasSize(1);
    }

    private DocumentChunk chunk(Long id, Long documentId, int index, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setUserId(1L);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setEmbedding(VectorUtils.serialize(embeddingModel.embed(content)));
        return chunk;
    }

    private Document document(Long id, String name) {
        Document document = new Document();
        document.setId(id);
        document.setName(name);
        return document;
    }
}
