package com.louitarot.card.application.port.out;

import com.louitarot.card.adapter.out.persistence.CardInterpretationEntity;

import java.util.Optional;

/**
 * 케미 뽑기용 범용(주제 없음) 카드 해석 캐시를 읽어오기 위한 아웃바운드 포트.
 * 아직 AI 연동 전이라 해석이 비어있는 카드가 많을 수 있다 — 그건 정상 상태이고,
 * 호출부(CardService)는 Optional.empty()를 "해석이 아직 준비 안 됨"으로 처리한다(에러 아님).
 */
public interface LoadCardInterpretationPort {

    Optional<CardInterpretationEntity> loadInterpretation(Short cardId, boolean reversed);
}
