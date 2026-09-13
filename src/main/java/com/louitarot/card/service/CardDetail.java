package com.louitarot.card.service;

import com.louitarot.card.entity.CardEntity;
import com.louitarot.common.domain.Topic;

import java.util.Map;

/**
 * CardService.getCard()의 결과물. 카드 자체(CardEntity) + 정/역방향 해석 텍스트,
 * 주제별(연애/취업/재물) 해석까지 한데 묶은 것. /card/[slug] SEO 상세 페이지가
 * 카드 사전처럼 충분히 긴 콘텐츠를 갖추도록 즉시 생성(getOrGenerate)해서 채우므로
 * 여기 필드들은 null이 아니다.
 */
public record CardDetail(
        CardEntity card,
        String uprightInterpretation,
        String reversedInterpretation,
        Map<Topic, String> uprightTopicInterpretations,
        Map<Topic, String> reversedTopicInterpretations
) {
}
