package com.louitarot.common.response;

/**
 * API 명세.md의 목록 조회 페이지네이션 메타 정보. 목록이 아닌 응답은 이 필드 자체가 null.
 */
public record ApiMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ApiMeta of(int page, int size, long totalElements) {
        int totalPages = (int) Math.max(1, Math.ceil((double) totalElements / size));
        return new ApiMeta(page, size, totalElements, totalPages);
    }
}
