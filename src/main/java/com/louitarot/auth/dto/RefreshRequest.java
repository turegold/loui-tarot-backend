package com.louitarot.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /auth/refresh 요청 바디. */
public record RefreshRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.") String refreshToken
) {
}
