package com.louitarot.card.application;

import com.louitarot.card.adapter.out.persistence.CardEntity;

/**
 * GetCardUseCase의 결과물. 카드 자체(CardEntity) + 정/역방향 해석 텍스트를 한데 묶은 것.
 * 해석 캐시가 아직 없는 카드는 upright/reversedInterpretation이 null일 수 있다 — 이건 에러가
 * 아니라 "아직 AI로 안 만들어졌다"는 정상 상태다 (AI 연동은 이번 범위 밖).
 */
public record CardDetail(
        CardEntity card,
        String uprightInterpretation,
        String reversedInterpretation
) {
}
