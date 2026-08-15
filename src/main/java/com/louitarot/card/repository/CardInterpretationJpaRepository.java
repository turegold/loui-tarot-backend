package com.louitarot.card.repository;

import com.louitarot.card.entity.CardInterpretationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 케미 뽑기용 범용 해석 캐시 조회. (카드, 방향) 조합이 유니크 키다. */
public interface CardInterpretationJpaRepository extends JpaRepository<CardInterpretationEntity, Long> {

    Optional<CardInterpretationEntity> findByCardIdAndReversed(Short cardId, boolean reversed);
}
