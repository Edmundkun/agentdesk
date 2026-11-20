package com.agentdesk.security;

import com.agentdesk.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 中读取当前登录用户，避免 Controller 层层传参。
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw BizException.unauthorized("登录已过期，请重新登录");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }
}
