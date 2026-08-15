package com.louitarot.card.repository;

import com.louitarot.card.entity.CardTopicInterpretationEntity;
import com.louitarot.common.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 개인 카드 뽑기용 주제별 해석 캐시 조회. (카드, 방향, 주제) 조합이 유니크 키다. */
public interface CardTopicInterpretationJpaRepository extends JpaRepository<CardTopicInterpretationEntity, Long> {

    Optional<CardTopicInterpretationEntity> findByCardIdAndReversedAndTopic(Short cardId, boolean reversed, Topic topic);
}
