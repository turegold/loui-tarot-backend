package com.louitarot.common.response;

/**
 * API 명세.md의 목록 조회 페이지네이션 메타 정보. 목록이 아닌 응답은 이 필드 자체가 null
 * (ApiResponse.success(data) — meta 없는 버전 — 을 쓰면 됨).
 */
public record ApiMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * page/size/totalElements만 넘기면 totalPages는 여기서 계산해준다.
     * 호출부가 매번 올림 나눗셈을 직접 하다가 실수하는 걸 막기 위해 이 메서드 안에 계산 로직을 둠.
     * 예: totalElements=57, size=20 → 57/20=2.85 → 올림해서 3페이지.
     */
    public static ApiMeta of(int page, int size, long totalElements) {
        int totalPages = (int) Math.max(1, Math.ceil((double) totalElements / size));
        return new ApiMeta(page, size, totalElements, totalPages);
    }
}
