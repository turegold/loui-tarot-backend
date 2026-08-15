package com.louitarot.common.exception;

/**
 * 도메인/유스케이스 코드에서 던지는 예외. ErrorCode 하나를 반드시 들고 다닌다.
 *
 * 도메인 코드는 이렇게만 쓰면 된다: throw new CustomException(ErrorCode.CARD_NOT_FOUND);
 * HTTP 상태코드가 몇 번인지, 응답 JSON이 어떻게 생겼는지는 도메인 코드가 전혀 몰라도 된다 —
 * 그건 이 예외를 최종적으로 잡는 GlobalExceptionHandler의 책임. (도메인 ↔ 웹 계층 분리)
 *
 * RuntimeException을 상속했다는 건 "checked exception이 아니다" = 호출부가 매번
 * try-catch나 throws 선언을 강제로 안 해도 된다는 뜻. 어차피 GlobalExceptionHandler가
 * 최상단에서 다 잡아주기 때문에, 중간의 서비스/컨트롤러 계층에서 일일이 처리할 필요가 없다.
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 기본 메시지(ErrorCode에 이미 정의된 것)를 그대로 쓸 때. 대부분 이걸 씀. */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** 상황에 맞는 메시지를 따로 넣고 싶을 때 (예: 어떤 slug가 없는지 구체적으로 알려주고 싶을 때). */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
