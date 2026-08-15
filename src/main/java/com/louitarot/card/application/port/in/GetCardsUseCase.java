package com.louitarot.card.application.port.in;

import com.louitarot.card.adapter.out.persistence.CardEntity;

import java.util.List;

/** [[API 명세]]의 GET /cards — 78장 전체 목록 조회. */
public interface GetCardsUseCase {

    List<CardEntity> getCards();
}
