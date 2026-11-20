package com.agentdesk.user.dto;

public record AuthResponse(String token, UserVO user) {
}
