package com.agentdesk.user;

import com.agentdesk.common.BizException;
import com.agentdesk.security.JwtTokenProvider;
import com.agentdesk.user.dto.AuthResponse;
import com.agentdesk.user.dto.LoginRequest;
import com.agentdesk.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("注册成功返回 token 与用户信息，密码以 BCrypt 存储")
    void shouldRegisterSuccessfully() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });
        when(jwtTokenProvider.createToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse response = userService.register(new RegisterRequest("alice", "123456", "Alice"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("alice");
        assertThat(response.user().nickname()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("用户名重复时拒绝注册")
    void shouldRejectDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("demo", "123456", "Demo")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已被注册");
    }

    @Test
    @DisplayName("密码错误时返回统一的登录失败提示")
    void shouldRejectWrongPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setPassword(passwordEncoder.encode("123456"));
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login(new LoginRequest("demo", "wrong-password")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    @DisplayName("用户不存在时同样返回统一提示，避免账号枚举")
    void shouldRejectUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userService.login(new LoginRequest("nobody", "123456")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名或密码错误");
    }
}
