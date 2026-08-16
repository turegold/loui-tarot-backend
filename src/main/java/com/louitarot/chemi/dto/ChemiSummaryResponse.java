package com.louitarot.chemi.dto;

import com.louitarot.card.dto.CardBriefResponse;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.chemi.entity.ChemiDrawEntity;

import java.time.LocalDateTime;

/** [[API 명세]]의 GET /users/me/chemi-draws 목록 항목 하나 — 로그인 방장으로 뽑은 케미 기록. */
public record ChemiSummaryResponse(
        String slug,
        CardBriefResponse card,
        boolean isReversed,
        LocalDateTime createdAt
) {

    public static ChemiSummaryResponse from(ChemiDrawEntity draw, CardEntity card) {
        return new ChemiSummaryResponse(draw.getSlug(), CardBriefResponse.from(card), draw.isReversed(), draw.getCreatedAt());
    }
}
