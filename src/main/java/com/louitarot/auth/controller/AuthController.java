package com.louitarot.auth.controller;

import com.louitarot.auth.dto.AccessTokenResponse;
import com.louitarot.auth.dto.KakaoCallbackRequest;
import com.louitarot.auth.dto.LoginResponse;
import com.louitarot.auth.dto.RefreshRequest;
import com.louitarot.auth.service.AuthService;
import com.louitarot.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [[API 명세]]의 인증(Auth) 엔드포인트. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/kakao/callback")
    public ApiResponse<LoginResponse> kakaoCallback(@Valid @RequestBody KakaoCallbackRequest request) {
        return ApiResponse.success(LoginResponse.from(authService.loginWithKakao(request.authorizationCode())));
    }

    @PostMapping("/refresh")
    public ApiResponse<AccessTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(new AccessTokenResponse(authService.reissueAccessToken(request.refreshToken())));
    }

    /** SecurityConfig에서 이 경로는 인증 필요로 막아두기 때문에, 여기 도달했다는 것 자체가
     * JwtAuthenticationFilter가 이미 유효한 액세스 토큰에서 userId를 뽑아 인증을 채워놨다는 뜻이다. */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        authService.logout((Long) authentication.getPrincipal());
        return ApiResponse.success(null);
    }
}
