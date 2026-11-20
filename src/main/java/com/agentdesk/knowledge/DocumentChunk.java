package com.agentdesk.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档切片：content 为原文，embedding 为序列化后的向量（逗号分隔），检索时在内存中做余弦计算。
 */
@Data
@TableName("document_chunks")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long userId;

    private Integer chunkIndex;

    private String content;

    private String embedding;

    private LocalDateTime createdAt;
}
