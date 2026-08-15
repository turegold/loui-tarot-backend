package com.louitarot.common.exception;

import org.springframework.http.HttpStatus;

/**
 * API 명세.md의 에러 코드 테이블을 그대로 옮긴 것. 새 에러 상황이 생기면 여기 먼저 추가하고
 * (API 명세.md도 같이 갱신) 구현한다 — 코드에서 즉흥적으로 문자열을 만들지 않는다.
 *
 * 핵심 의도: "이 에러의 HTTP 상태코드가 뭐고 기본 메시지가 뭐냐"는 정보가 코드베이스 여기저기
 * 흩어지지 않고 이 enum 한 곳에만 존재하게 만드는 것. CustomException이 ErrorCode 하나만 들고
 * 다니면, GlobalExceptionHandler가 거기서 상태코드+코드명+메시지를 전부 꺼내 쓸 수 있다.
 */
public enum ErrorCode {

    // 공통
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 파라미터 검증에 실패했습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 서버 오류가 발생했습니다."),

    // 인증
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_KAKAO_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "카카오 로그인에 실패했습니다."),

    // 카드 / 개인 카드 뽑기 / 케미 뽑기 도메인
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드입니다."),
    FORTUNE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 개인 카드 뽑기 결과입니다."),
    CHEMI_DRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 케미 뽑기 결과입니다."),
    CHEMI_HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 방장 링크입니다."),

    // 외부 API / 트래픽 제어
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    AI_INTERPRETATION_FAILED(HttpStatus.BAD_GATEWAY, "AI 해석 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    // enum 상수를 선언할 때(위 목록) 괄호 안 값이 그대로 이 생성자로 들어온다.
    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
