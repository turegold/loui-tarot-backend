package com.louitarot.auth.dto;

import com.louitarot.auth.service.LoginResult;

/** POST /auth/kakao/callback 응답 바디. */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserSummaryResponse user,
        boolean isNewUser
) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.refreshToken(),
                UserSummaryResponse.from(result.user()),
                result.isNewUser());
    }
}
