package com.louitarot.auth.application.port.out;

/**
 * 카카오 인가코드를 사용자 정보로 바꾸는 흐름(토큰 교환 + 프로필 조회)의 추상화.
 *
 * 헥사고날을 여기에만 적용한 이유(백엔드 설계 원칙 1번): 이 자리는 원래 인터페이스가 없는
 * raw HTTP 호출이라, 포트를 두면 AuthService가 실제 카카오 서버 없이도(가짜 구현체로) 테스트
 * 가능해지고, 카카오 SDK/응답 구조 같은 세부사항이 AuthService로 새어 들어오지 않는다.
 */
public interface KakaoAuthPort {

    /**
     * 인가코드로 카카오 액세스 토큰을 발급받고, 그 토큰으로 사용자 정보까지 조회한다.
     * 실패(코드 만료/위조, 카카오 서버 오류 등)하면 CustomException(AUTH_KAKAO_LOGIN_FAILED).
     */
    KakaoUserInfo fetchUserInfo(String authorizationCode);
}
