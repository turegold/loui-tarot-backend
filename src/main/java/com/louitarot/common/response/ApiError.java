package com.louitarot.common.response;

import java.util.List;

/**
 * API 명세.md의 실패 응답 {@code error} 필드. {@code details}는 필드 단위 검증 실패 시에만 채워진다.
 */
public record ApiError(
        String code,
        String message,
        List<FieldDetail> details
) {

    public record FieldDetail(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<FieldDetail> details) {
        return new ApiError(code, message, details);
    }
}
