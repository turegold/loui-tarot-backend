package com.louitarot.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /auth/kakao/callback 요청 바디. */
public record KakaoCallbackRequest(
        @NotBlank(message = "인가코드는 필수입니다.") String authorizationCode
) {
}
