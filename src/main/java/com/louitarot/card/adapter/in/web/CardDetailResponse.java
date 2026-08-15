package com.louitarot.card.adapter.in.web;

import com.louitarot.card.adapter.out.persistence.CardEntity;
import com.louitarot.card.application.CardDetail;
import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.Element;
import com.louitarot.card.domain.Suit;

/** [[API 명세]] GET /cards/{cardId}의 응답 형태. seoSlug는 상세에서는 안 내려준다(스펙 그대로). */
public record CardDetailResponse(
        Short id,
        String nameKr,
        String nameEn,
        ArcanaType arcanaType,
        Suit suit,
        Element element,
        String imageUrl,
        String uprightInterpretation,
        String reversedInterpretation
) {

    public static CardDetailResponse from(CardDetail detail) {
        CardEntity card = detail.card();
        return new CardDetailResponse(
                card.getId(), card.getNameKr(), card.getNameEn(), card.getArcanaType(),
                card.getSuit(), card.getElement(), card.getImageUrl(),
                detail.uprightInterpretation(), detail.reversedInterpretation());
    }
}
