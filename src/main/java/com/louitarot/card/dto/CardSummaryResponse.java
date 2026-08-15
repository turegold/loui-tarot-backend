package com.louitarot.card.dto;

import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.Element;
import com.louitarot.card.domain.Suit;

/** [[API 명세]] GET /cards의 목록 항목 하나. 해석 텍스트는 포함하지 않는다(목록에서는 불필요). */
public record CardSummaryResponse(
        Short id,
        String nameKr,
        String nameEn,
        ArcanaType arcanaType,
        Suit suit,
        Element element,
        String imageUrl,
        String seoSlug
) {

    public static CardSummaryResponse from(CardEntity card) {
        return new CardSummaryResponse(
                card.getId(), card.getNameKr(), card.getNameEn(), card.getArcanaType(),
                card.getSuit(), card.getElement(), card.getImageUrl(), card.getSeoSlug());
    }
}
