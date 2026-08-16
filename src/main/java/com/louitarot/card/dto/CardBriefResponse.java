package com.louitarot.card.dto;

import com.louitarot.card.entity.CardEntity;

/**
 * 케미/개인 카드 뽑기 응답에 카드를 요약해서 실을 때 쓰는 최소 형태([[API 명세]] 기준 id/nameKr/imageUrl만).
 * 카드 목록/상세(GET /cards)의 {@link CardSummaryResponse}보다 필드가 적다 — 뽑기 결과 화면은
 * 카드 속성(아르카나/수트/원소) 전부를 보여줄 필요가 없어서다.
 */
public record CardBriefResponse(Short id, String nameKr, String imageUrl) {

    public static CardBriefResponse from(CardEntity card) {
        return new CardBriefResponse(card.getId(), card.getNameKr(), card.getImageUrl());
    }
}
