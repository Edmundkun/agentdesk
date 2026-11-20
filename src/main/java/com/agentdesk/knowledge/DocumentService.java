package com.agentdesk.knowledge;

import com.agentdesk.common.BizException;
import com.agentdesk.config.AppProperties;
import com.agentdesk.embedding.EmbeddingModel;
import com.agentdesk.embedding.VectorUtils;
import com.agentdesk.knowledge.dto.DocumentVO;
import com.agentdesk.knowledge.dto.KnowledgeStats;
import com.agentdesk.knowledge.dto.SearchHit;
import com.agentdesk.knowledge.dto.SearchRequest;
import com.agentdesk.retrieval.RetrievalService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of("txt", "md", "pdf");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final DocumentParser documentParser;
    private final EmbeddingModel embeddingModel;
    private final RetrievalService retrievalService;
    private final AppProperties properties;

    /**
     * 上传入口：校验空文件后转交字节流入库，便于测试与演示数据初始化复用。
     */
    @Transactional
    public DocumentVO ingest(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw BizException.badRequest("读取文件失败：" + e.getMessage());
        }
        return ingest(userId, file.getOriginalFilename(), bytes);
    }

    /**
     * 文档入库：解析 → 切片 → 向量化 → 落库，同步执行（演示文档单份约 50~200ms）。
     */
    @Transactional
    public DocumentVO ingest(Long userId, String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw BizException.badRequest("文件内容为空");
        }
        if (bytes.length > MAX_SIZE_BYTES) {
            throw BizException.badRequest("文件不能超过 10MB");
        }
        String name = sanitizeName(fileName);
        String extension = extensionOf(name);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw BizException.badRequest("仅支持 txt / md / pdf 格式");
        }

        String text = documentParser.parse(extension, bytes);
        if (text == null || text.isBlank()) {
            throw BizException.badRequest("文档内容为空或无法解析出文字");
        }

        List<String> chunks = TextChunker.split(text, properties.getRag().getChunkSize(),
                properties.getRag().getChunkOverlap());
        if (chunks.isEmpty()) {
            throw BizException.badRequest("文档内容过短，无法切分");
        }

        Document document = new Document();
        document.setUserId(userId);
        document.setName(name);
        document.setFileType(extension);
        document.setSizeBytes((long) bytes.length);
        document.setChunkCount(chunks.size());
        document.setStatus("READY");
        document.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(document);

        List<DocumentChunk> entities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setUserId(userId);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            // 向量化时带上文档名，提升“按文档名提问”的召回效果
            chunk.setEmbedding(VectorUtils.serialize(embeddingModel.embed(name + "\n" + content)));
            chunk.setCreatedAt(LocalDateTime.now());
            entities.add(chunk);
        }
        entities.forEach(chunkMapper::insert);

        log.info("文档入库完成 userId={} name={} chunks={} chars={}", userId, name, chunks.size(), text.length());
        return DocumentVO.from(document);
    }

    public List<DocumentVO> list(Long userId) {
        return documentMapper.selectList(Wrappers.<Document>lambdaQuery()
                        .eq(Document::getUserId, userId)
                        .orderByDesc(Document::getId))
                .stream()
                .map(DocumentVO::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || !document.getUserId().equals(userId)) {
            throw BizException.notFound("文档不存在");
        }
        chunkMapper.delete(Wrappers.<DocumentChunk>lambdaQuery().eq(DocumentChunk::getDocumentId, documentId));
        documentMapper.deleteById(documentId);
    }

    public KnowledgeStats stats(Long userId) {
        Long documents = documentMapper.selectCount(Wrappers.<Document>lambdaQuery()
                .eq(Document::getUserId, userId));
        Long chunks = chunkMapper.selectCount(Wrappers.<DocumentChunk>lambdaQuery()
                .eq(DocumentChunk::getUserId, userId));
        long chars = chunkMapper.sumContentLength(userId);
        return new KnowledgeStats(documents == null ? 0 : documents, chunks == null ? 0 : chunks, chars);
    }

    public List<SearchHit> search(Long userId, SearchRequest request) {
        int topK = request.topKOrDefault(properties.getRag().getTopK());
        return retrievalService.search(userId, request.query(), topK).stream()
                .map(hit -> new SearchHit(hit.documentId(), hit.documentName(), hit.chunkIndex(),
                        hit.content(), hit.score()))
                .toList();
    }

    private String sanitizeName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "未命名文档.txt";
        }
        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw BizException.badRequest("文件缺少扩展名，仅支持 txt / md / pdf");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
