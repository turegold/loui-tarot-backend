package com.louitarot.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 리프레시 토큰을 Redis에 중앙화해서 저장한다 — 인스턴스 로컬 메모리에 상태를 두지 않는다는
 * 스케일 아웃 원칙(백엔드 설계 원칙 2번)을 인증에도 그대로 적용한 것.
 *
 * 유저 1명당 키 1개만 유지한다 — 재로그인하면 이전 리프레시 토큰은 자동으로 덮어써져 무효화된다.
 * 여러 기기 동시 로그인 지원은 MVP 범위 밖(알려진 제약으로 남겨둠).
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, Duration.ofMillis(refreshTokenExpirationMs));
    }

    /** 저장된 값과 일치하는지 확인. 로그아웃됐거나 다른 로그인으로 덮어써졌으면 false. */
    public boolean matches(Long userId, String refreshToken) {
        return refreshToken.equals(redisTemplate.opsForValue().get(key(userId)));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
