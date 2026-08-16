package com.louitarot.auth.service;

import com.louitarot.auth.entity.UserEntity;

/** POST /auth/kakao/callback 응답을 만들기 위해 AuthService가 컨트롤러에 넘기는 결과. */
public record LoginResult(String accessToken, String refreshToken, UserEntity user, boolean isNewUser) {
}
