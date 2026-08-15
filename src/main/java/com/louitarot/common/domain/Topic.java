package com.louitarot.common.domain;

/**
 * 개인 카드 뽑기의 주제 (종합운/연애운/취업운/재물운).
 *
 * fortune(뽑기 기록)과 card(주제별 해석 캐시) 양쪽에서 쓰이는 값이라 common에 둔다.
 * 케미 뽑기는 주제 개념이 없다 — 범용 해석만 사용 ([[02 주요 기능 정의]]).
 */
public enum Topic {
    COMPREHENSIVE,
    LOVE,
    CAREER,
    WEALTH
}
