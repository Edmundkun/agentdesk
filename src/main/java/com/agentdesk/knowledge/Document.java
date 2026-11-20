package com.agentdesk.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("documents")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String fileType;

    private Long sizeBytes;

    private Integer chunkCount;

    private String status;

    private LocalDateTime createdAt;
}
