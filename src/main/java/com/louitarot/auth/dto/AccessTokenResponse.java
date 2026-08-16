package com.louitarot.auth.dto;

/** POST /auth/refresh 응답 바디. */
public record AccessTokenResponse(String accessToken) {
}
