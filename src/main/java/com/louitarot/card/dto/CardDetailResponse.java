package com.louitarot.card.dto;

import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.CardEssence;
import com.louitarot.card.domain.CardEssenceCatalog;
import com.louitarot.card.domain.Element;
import com.louitarot.card.domain.Suit;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.service.CardDetail;
import com.louitarot.common.domain.Topic;

/**
 * [[API 명세]] GET /cards/{cardId}의 응답 형태. seoSlug는 상세에서는 안 내려준다(스펙 그대로).
 * upright/reversedKeyword와 주제별(연애/취업/재물) 해석은 /card/[slug] SEO 상세 페이지를
 * 카드 사전처럼 충분히 긴 콘텐츠로 채우기 위해 추가됐다 — 실제 유저 내비게이션(카드 뽑기
 * 결과 화면 등)에서도 "이 카드 더 알아보기"로 연결되는, 진짜로 유저가 보는 페이지다.
 */
public record CardDetailResponse(
        Short id,
        String nameKr,
        String nameEn,
        ArcanaType arcanaType,
        Suit suit,
        Element element,
        Short number,
        String imageUrl,
        String uprightInterpretation,
        String reversedInterpretation,
        String uprightKeyword,
        String reversedKeyword,
        String uprightLoveInterpretation,
        String uprightCareerInterpretation,
        String uprightWealthInterpretation,
        String reversedLoveInterpretation,
        String reversedCareerInterpretation,
        String reversedWealthInterpretation
) {

    public static CardDetailResponse from(CardDetail detail) {
        CardEntity card = detail.card();
        CardEssence uprightEssence = CardEssenceCatalog.resolve(card, false);
        CardEssence reversedEssence = CardEssenceCatalog.resolve(card, true);
        var uprightTopics = detail.uprightTopicInterpretations();
        var reversedTopics = detail.reversedTopicInterpretations();

        return new CardDetailResponse(
                card.getId(), card.getNameKr(), card.getNameEn(), card.getArcanaType(),
                card.getSuit(), card.getElement(), card.getNumber(), card.getImageUrl(),
                detail.uprightInterpretation(), detail.reversedInterpretation(),
                uprightEssence.keyword(), reversedEssence.keyword(),
                uprightTopics.get(Topic.LOVE), uprightTopics.get(Topic.CAREER), uprightTopics.get(Topic.WEALTH),
                reversedTopics.get(Topic.LOVE), reversedTopics.get(Topic.CAREER), reversedTopics.get(Topic.WEALTH));
    }
}
