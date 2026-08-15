package com.louitarot.common.response;

import java.util.List;

/**
 * API 명세.md의 실패 응답 {@code error} 필드.
 * ApiResponse.fail(...)에 담겨서 나가는 것 외에는 이 타입이 단독으로 쓰일 일은 거의 없다.
 */
public record ApiError(
        String code,       // ErrorCode의 name() 그대로 (예: "CARD_NOT_FOUND")
        String message,    // 사람이 읽는 에러 메시지
        List<FieldDetail> details  // 필드 단위 검증 실패일 때만 채워짐, 그 외엔 null
) {

    /** ApiError 전용 부속 타입이라는 걸 구조로 드러내기 위해 중첩 record로 선언. */
    public record FieldDetail(String field, String message) {
    }

    /** details 없이 code+message만 있는 일반적인 실패. */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    /** @Valid 검증 실패처럼 필드별 상세 정보가 있는 경우. */
    public static ApiError of(String code, String message, List<FieldDetail> details) {
        return new ApiError(code, message, details);
    }
}
