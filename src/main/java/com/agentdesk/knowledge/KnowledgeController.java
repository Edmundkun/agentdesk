package com.agentdesk.knowledge;

import com.agentdesk.common.ApiResponse;
import com.agentdesk.knowledge.dto.DocumentVO;
import com.agentdesk.knowledge.dto.KnowledgeStats;
import com.agentdesk.knowledge.dto.SearchHit;
import com.agentdesk.knowledge.dto.SearchRequest;
import com.agentdesk.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "知识库")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档（自动切片 + 向量化）")
    @PostMapping("/documents")
    public ApiResponse<DocumentVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.ingest(AuthUtils.currentUserId(), file));
    }

    @Operation(summary = "文档列表")
    @GetMapping("/documents")
    public ApiResponse<List<DocumentVO>> list() {
        return ApiResponse.ok(documentService.list(AuthUtils.currentUserId()));
    }

    @Operation(summary = "删除文档（级联删除切片）")
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(AuthUtils.currentUserId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "知识库统计")
    @GetMapping("/stats")
    public ApiResponse<KnowledgeStats> stats() {
        return ApiResponse.ok(documentService.stats(AuthUtils.currentUserId()));
    }

    @Operation(summary = "检索测试（混合检索）")
    @PostMapping("/search")
    public ApiResponse<List<SearchHit>> search(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.ok(documentService.search(AuthUtils.currentUserId(), request));
    }
}
