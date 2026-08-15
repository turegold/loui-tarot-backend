package com.louitarot.common.response;

/**
 * API 명세.md의 공통 응답 포맷 — {@code success/data/error/meta}.
 * 모든 컨트롤러가 이 타입 하나로 응답한다 (GlobalExceptionHandler도 실패 시 이걸 반환).
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        ApiMeta meta
) {

    // ↓ 정적 팩토리 메서드 3개. new ApiResponse<>(...)를 직접 쓰지 않고 이걸 거치게 하는 이유:
    //   "성공이면 error는 항상 null" 같은 규칙을 메서드 이름으로 드러내고, 호출부 실수를 줄이기 위함.
    //   (다만 record의 생성자 자체는 여전히 public이라 강제는 아님 — new로 우회하면 여전히 가능)

    /** 목록이 아닌 성공 응답. meta는 null. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** 목록 성공 응답. meta에 페이지네이션 정보(ApiMeta)를 함께 싣는다. */
    public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    /** 실패 응답. data는 항상 null, error에 원인을 담는다. */
    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
