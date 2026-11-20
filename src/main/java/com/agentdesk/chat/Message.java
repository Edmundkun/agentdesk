package com.agentdesk.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    /**
     * user / assistant
     */
    private String role;

    private String content;

    /**
     * Agent 推理轨迹 JSON，用于前端还原每一步工具调用。
     */
    private String traceJson;

    private LocalDateTime createdAt;
}
