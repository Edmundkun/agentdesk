package com.agentdesk.chat.dto;

import com.agentdesk.chat.Conversation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ConversationVO(
        Long id,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt,
        long messageCount) {

    public static ConversationVO from(Conversation conversation, long messageCount) {
        return new ConversationVO(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(), messageCount);
    }
}
