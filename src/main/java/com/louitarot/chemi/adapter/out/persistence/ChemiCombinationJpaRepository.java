package com.louitarot.chemi.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * chemi_combinations(카드 조합 캐시) 조회.
 *
 * 조회할 때도 저장할 때와 동일하게 (a ≤ b)로 정규화된 순서를 넘겨야 캐시가 맞는다
 * — ChemiCombinationEntity.normalize()와 같은 규칙 ([[AI 해석 캐싱 전략]]).
 */
public interface ChemiCombinationJpaRepository extends JpaRepository<ChemiCombinationEntity, Long> {

    Optional<ChemiCombinationEntity> findByCardAIdAndReversedAAndCardBIdAndReversedB(
            Short cardAId, boolean reversedA, Short cardBId, boolean reversedB);
}
