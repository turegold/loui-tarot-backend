package com.louitarot.auth.application.port.out;

/**
 * 카카오에서 받아온 사용자 정보 중 우리가 실제로 쓰는 것만 담은 도메인 형태.
 * 카카오 API 응답 구조(kakao_account.profile.nickname 같은 중첩)를 AuthService가 몰라도 되게 한다.
 */
public record KakaoUserInfo(Long kakaoId, String nickname, String profileImageUrl) {
}
