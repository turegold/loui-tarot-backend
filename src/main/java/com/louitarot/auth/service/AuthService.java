package com.louitarot.auth.service;

import com.louitarot.auth.application.port.out.KakaoAuthPort;
import com.louitarot.auth.application.port.out.KakaoUserInfo;
import com.louitarot.auth.entity.UserEntity;
import com.louitarot.auth.repository.UserJpaRepository;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import com.louitarot.common.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 인증 흐름 오케스트레이션(레이어드) — 실제 카카오 HTTP 호출은 KakaoAuthPort 뒤에,
 * JWT 서명/검증은 JwtTokenProvider에 맡기고, 여긴 "로그인/재발급/로그아웃이 각각 어떤 순서로
 * 일어나는가"만 담당한다.
 */
@Service
public class AuthService {

    private final KakaoAuthPort kakaoAuthPort;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            KakaoAuthPort kakaoAuthPort,
            UserJpaRepository userJpaRepository,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.kakaoAuthPort = kakaoAuthPort;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    /** 최초 로그인이면 회원가입까지 자동으로 처리한다 ([[API 명세]]의 isNewUser). */
    @Transactional
    public LoginResult loginWithKakao(String authorizationCode) {
        KakaoUserInfo kakaoUserInfo = kakaoAuthPort.fetchUserInfo(authorizationCode);
        Optional<UserEntity> existingUser = userJpaRepository.findByKakaoId(kakaoUserInfo.kakaoId());

        boolean isNewUser = existingUser.isEmpty();
        UserEntity user = existingUser.orElseGet(() -> userJpaRepository.save(new UserEntity(
                kakaoUserInfo.kakaoId(), kakaoUserInfo.nickname(), kakaoUserInfo.profileImageUrl())));
        if (!isNewUser) {
            user.recordLogin();
        }

        return issueTokens(user, isNewUser);
    }

    /** 리프레시 토큰이 유효하고 Redis에 저장된 값과 일치할 때만 새 액세스 토큰을 내준다. */
    @Transactional(readOnly = true)
    public String reissueAccessToken(String refreshToken) {
        Long userId = parseRefreshTokenOrThrow(refreshToken);
        if (!refreshTokenService.matches(userId, refreshToken)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return jwtTokenProvider.generateAccessToken(userId);
    }

    /** Redis에서 리프레시 토큰을 지워 무효화한다. 액세스 토큰은 만료 전까지는 여전히 유효(단기 토큰이라 허용). */
    public void logout(Long userId) {
        refreshTokenService.delete(userId);
    }

    private LoginResult issueTokens(UserEntity user, boolean isNewUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return new LoginResult(accessToken, refreshToken, user, isNewUser);
    }

    private Long parseRefreshTokenOrThrow(String refreshToken) {
        try {
            return jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
