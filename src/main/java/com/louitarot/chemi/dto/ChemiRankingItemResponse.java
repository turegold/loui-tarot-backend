package com.louitarot.chemi.dto;

import com.louitarot.card.dto.CardBriefResponse;

/** [[API 명세]]의 {@code GET /chemi-draws/{hostSlug}/ranking} 목록 항목 하나. */
public record ChemiRankingItemResponse(String guestNickname, CardBriefResponse guestCard, short score) {
}
