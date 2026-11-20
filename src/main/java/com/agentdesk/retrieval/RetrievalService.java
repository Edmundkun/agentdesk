package com.agentdesk.retrieval;

import com.agentdesk.config.AppProperties;
import com.agentdesk.embedding.EmbeddingModel;
import com.agentdesk.embedding.TextTokenizer;
import com.agentdesk.embedding.VectorUtils;
import com.agentdesk.knowledge.Document;
import com.agentdesk.knowledge.DocumentChunk;
import com.agentdesk.knowledge.DocumentChunkMapper;
import com.agentdesk.knowledge.DocumentMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 混合检索：向量余弦相似度 + 关键词命中率加权。
 *
 * <p>纯向量检索在短查询、专有名词（如“差旅报销标准”）上容易漂移，叠加关键词得分可以显著提升准确率；
 * 结果使用 Caffeine 做 5 分钟本地缓存，重复提问不再重复计算。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final DocumentChunkMapper chunkMapper;
    private final DocumentMapper documentMapper;
    private final EmbeddingModel embeddingModel;
    private final AppProperties properties;

    private final Cache<String, List<RetrievedChunk>> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public List<RetrievedChunk> search(Long userId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(topK, 10));
        String key = userId + "::" + limit + "::" + query.trim().toLowerCase();
        return cache.get(key, ignored -> doSearch(userId, query.trim(), limit));
    }

    private List<RetrievedChunk> doSearch(Long userId, String query, int topK) {
        List<DocumentChunk> chunks = chunkMapper.selectList(Wrappers.<DocumentChunk>lambdaQuery()
                .eq(DocumentChunk::getUserId, userId));
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, String> documentNames = documentMapper.selectBatchIds(
                        chunks.stream().map(DocumentChunk::getDocumentId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Document::getId, Document::getName, (a, b) -> a));

        float[] queryVector = embeddingModel.embed(query);
        Set<String> queryTokens = new HashSet<>(TextTokenizer.distinctTokens(query));
        double keywordWeight = clamp(properties.getRag().getKeywordWeight(), 0d, 1d);
        double minScore = properties.getRag().getMinScore();

        // 计算查询词的文档频率，越"稀有"的词权重越高（BM25 的 IDF 思想）
        Map<String, Double> idf = new HashMap<>();
        double totalIdf = 0d;
        for (String token : queryTokens) {
            long df = chunks.stream().filter(chunk -> containsIgnoreCase(chunk.getContent(), token)).count();
            double weight = Math.log(1d + (double) chunks.size() / (1d + df));
            idf.put(token, weight);
            totalIdf += weight;
        }

        List<RetrievedChunk> hits = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            float[] chunkVector = VectorUtils.deserialize(chunk.getEmbedding());
            double cosine = Math.max(0d, VectorUtils.cosine(queryVector, chunkVector));
            double keywordScore = keywordScore(query, queryTokens, idf, totalIdf, chunk.getContent());
            double score = (1 - keywordWeight) * cosine + keywordWeight * keywordScore;
            if (score > 0 && chunk.getContent().contains(query)) {
                score = Math.min(1d, score + 0.05);
            }
            if (score >= minScore) {
                hits.add(new RetrievedChunk(chunk.getDocumentId(),
                        documentNames.getOrDefault(chunk.getDocumentId(), "未知文档"),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        Math.round(score * 10000d) / 10000d));
            }
        }

        return hits.stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private boolean containsIgnoreCase(String content, String token) {
        return content != null && content.toLowerCase().contains(token.toLowerCase());
    }

    /**
     * 关键词得分 = 命中词的 IDF 之和 / 查询词 IDF 总和，再对完全匹配给一点加成。
     */
    private double keywordScore(String query, Set<String> queryTokens, Map<String, Double> idf,
                                double totalIdf, String content) {
        if (queryTokens.isEmpty() || totalIdf == 0 || content == null || content.isBlank()) {
            return 0d;
        }
        String lower = content.toLowerCase();
        double hitIdf = queryTokens.stream()
                .filter(token -> lower.contains(token.toLowerCase()))
                .mapToDouble(token -> idf.getOrDefault(token, 0d))
                .sum();
        double coverage = hitIdf / totalIdf;
        double exactBonus = content.contains(query) ? 0.2 : 0d;
        return Math.min(1d, coverage + exactBonus);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
