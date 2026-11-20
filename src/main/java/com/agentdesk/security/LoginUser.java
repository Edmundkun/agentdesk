package com.agentdesk.security;

/**
 * 放入 Spring Security 上下文的登录用户快照（不包含密码）。
 */
public record LoginUser(Long id, String username, String nickname) {
}
