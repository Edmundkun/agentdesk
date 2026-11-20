package com.agentdesk.user;

import com.agentdesk.common.BizException;
import com.agentdesk.security.JwtTokenProvider;
import com.agentdesk.user.dto.AuthResponse;
import com.agentdesk.user.dto.LoginRequest;
import com.agentdesk.user.dto.RegisterRequest;
import com.agentdesk.user.dto.UserVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (exists != null && exists > 0) {
            throw BizException.badRequest("用户名已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank()
                ? request.username() : request.nickname().trim());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return buildAuth(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        return buildAuth(user);
    }

    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在，请重新登录");
        }
        return user;
    }

    public UserVO me(Long userId) {
        return UserVO.from(requireUser(userId));
    }

    private AuthResponse buildAuth(User user) {
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getNickname());
        return new AuthResponse(token, UserVO.from(user));
    }
}
