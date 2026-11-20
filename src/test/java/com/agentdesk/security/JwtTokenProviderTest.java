package com.agentdesk.security;

import com.agentdesk.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("agentdesk-unit-test-secret-key-1234567890");
        properties.getJwt().setExpireHours(2);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("签发后可解析出完整的登录用户")
    void shouldRoundTripToken() {
        String token = provider.createToken(9L, "alice", "Alice");

        LoginUser user = provider.parse(token);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(9L);
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.nickname()).isEqualTo("Alice");
        assertThat(provider.expireSeconds()).isEqualTo(7200);
    }

    @Test
    @DisplayName("被篡改的 token 解析失败并返回 null")
    void shouldRejectTamperedToken() {
        String token = provider.createToken(1L, "demo", "演示用户");

        assertThat(provider.parse(token + "tampered")).isNull();
        assertThat(provider.parse("not-a-jwt")).isNull();
    }

    @Test
    @DisplayName("密钥过短时启动即失败，避免弱密钥上线")
    void shouldRejectShortSecret() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("short");

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
