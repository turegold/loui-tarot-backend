package com.louitarot.common.exception;

import org.springframework.http.HttpStatus;

/**
 * API 명세.md의 에러 코드 테이블을 그대로 옮긴 것. 새 에러 상황이 생기면 여기 먼저 추가하고
 * (API 명세.md도 같이 갱신) 구현한다 — 코드에서 즉흥적으로 문자열을 만들지 않는다.
 */
public enum ErrorCode {

    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 파라미터 검증에 실패했습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 서버 오류가 발생했습니다."),

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_KAKAO_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "카카오 로그인에 실패했습니다."),

    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드입니다."),
    FORTUNE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 개인 카드 뽑기 결과입니다."),
    CHEMI_DRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 케미 뽑기 결과입니다."),
    CHEMI_HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 방장 링크입니다."),

    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    AI_INTERPRETATION_FAILED(HttpStatus.BAD_GATEWAY, "AI 해석 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

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
