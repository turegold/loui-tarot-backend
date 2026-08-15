package com.louitarot.card.application.port.out;

import com.louitarot.card.adapter.out.persistence.CardEntity;

import java.util.List;
import java.util.Optional;

/**
 * 카드 저장소에서 데이터를 읽어오기 위한 아웃바운드 포트.
 *
 * CardService(application 계층)는 이 인터페이스만 알고, 그 뒤가 JPA인지 아닌지는 모른다.
 * 실제 구현은 adapter/out/persistence의 CardPersistenceAdapter가 담당한다.
 * Repository 포트는 [[백엔드 설계 원칙]] 1번에서 헥사고날을 적용하기로 정한 범위 안이라 만든다
 * (AI API·카카오 로그인·Repository/Cache — 이 세 가지에만 좁혀서 적용).
 */
public interface LoadCardPort {

    List<CardEntity> loadAllCards();

    Optional<CardEntity> loadCard(Short cardId);
}
