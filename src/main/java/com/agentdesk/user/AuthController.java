package com.agentdesk.user;

import com.agentdesk.common.ApiResponse;
import com.agentdesk.security.AuthUtils;
import com.agentdesk.user.dto.AuthResponse;
import com.agentdesk.user.dto.LoginRequest;
import com.agentdesk.user.dto.RegisterRequest;
import com.agentdesk.user.dto.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "注册并自动登录")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.me(AuthUtils.currentUserId()));
    }
}
