package com.agentdesk.user.dto;

import com.agentdesk.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String nickname,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }
}
