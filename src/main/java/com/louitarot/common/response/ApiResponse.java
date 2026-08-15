package com.louitarot.common.response;

/**
 * API 명세.md의 공통 응답 포맷 — {@code success/data/error/meta}.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        ApiMeta meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
