package com.louitarot.fortune.dto;

import com.louitarot.card.dto.CardBriefResponse;

/** 개인 카드 뽑기 결과의 카드 한 장 (자리 라벨은 그 시점의 [[스프레드 테마]]로 이미 계산된 문자열). */
public record FortuneCardResponse(
        String positionLabel,
        CardBriefResponse card,
        boolean isReversed,
        String interpretation
) {
}
