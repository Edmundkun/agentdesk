package com.agentdesk.security;

import com.agentdesk.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与校验（HS256）。无状态鉴权，便于水平扩展。
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_NICKNAME = "nickname";

    private final SecretKey key;
    private final long expireMillis;

    public JwtTokenProvider(AppProperties properties) {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("agentdesk.jwt.secret 至少需要 32 个字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expireMillis = properties.getJwt().getExpireHours() * 3600_000L;
    }

    public String createToken(Long userId, String username, String nickname) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_NICKNAME, nickname)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expireMillis)))
                .signWith(key)
                .compact();
    }

    /**
     * @return 解析成功返回登录用户，token 非法/过期返回 null（由过滤器交给 EntryPoint 处理）
     */
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_USERNAME, String.class),
                    claims.get(CLAIM_NICKNAME, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long expireSeconds() {
        return expireMillis / 1000;
    }
}
