package com.louitarot.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 액세스/리프레시 토큰 발급 및 서명·만료 검증만 담당한다.
 *
 * 리프레시 토큰이 Redis에 저장된 값과 일치하는지(로그아웃 등으로 무효화됐는지)는 이 클래스의
 * 책임이 아니다 — 그건 RefreshTokenService가 담당한다. 여긴 순수하게 "이 토큰이 우리가 서명해서
 * 발급한 게 맞고, 아직 안 만료됐는지"만 본다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, TYPE_ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, TYPE_REFRESH, refreshTokenExpirationMs);
    }

    private String generateToken(Long userId, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 액세스 토큰이 아니거나(타입 불일치), 위조/만료됐으면 JwtException. */
    public Long parseAccessToken(String token) {
        return parseUserId(token, TYPE_ACCESS);
    }

    /** 리프레시 토큰이 아니거나, 위조/만료됐으면 JwtException. */
    public Long parseRefreshToken(String token) {
        return parseUserId(token, TYPE_REFRESH);
    }

    private Long parseUserId(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("유효하지 않은 토큰입니다.", e);
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("토큰 타입이 올바르지 않습니다. (expected=" + expectedType + ")");
        }
        return Long.valueOf(claims.getSubject());
    }
}
