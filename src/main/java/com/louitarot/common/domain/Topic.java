package com.louitarot.common.domain;

/**
 * 개인 카드 뽑기의 주제 (종합운/연애운/취업운/재물운).
 *
 * fortune(뽑기 기록)과 card(주제별 해석 캐시) 양쪽에서 쓰이는 값이라 common에 둔다.
 * 케미 뽑기는 주제 개념이 없다 — 범용 해석만 사용 ([[02 주요 기능 정의]]).
 */
public enum Topic {
    COMPREHENSIVE("종합운"),
    LOVE("연애운"),
    CAREER("취업운"),
    WEALTH("재물운");

    /** AI 프롬프트/해석 문구에 쓰는 한국어 표기. */
    private final String label;

    Topic(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
