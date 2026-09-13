package com.louitarot.card.service;

import com.louitarot.card.domain.CardInterpretationComposer;
import com.louitarot.card.domain.CardTopicInterpretationComposer;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.entity.CardInterpretationEntity;
import com.louitarot.card.entity.CardTopicInterpretationEntity;
import com.louitarot.card.repository.CardInterpretationJpaRepository;
import com.louitarot.card.repository.CardJpaRepository;
import com.louitarot.card.repository.CardTopicInterpretationJpaRepository;
import com.louitarot.common.domain.Topic;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 카드 조회 서비스. 일반적인 레이어드 스타일(Controller → Service → Repository)로 짠다 —
 * Spring Data Repository 자체가 이미 인터페이스라 그 위에 포트를 한 겹 더 두는 게 실익이 없어서,
 * DB 접근에는 헥사고날 포트/어댑터를 적용하지 않기로 했다. 포트는 AI API·카카오 로그인처럼
 * "원래 인터페이스가 없던 자리"에만 적용한다 ([[백엔드 설계 원칙]] 1번).
 */
@Service
public class CardService {

    /** /card/[slug] SEO 상세 페이지에서 정/역방향별로 함께 보여줄 주제. 종합운은 upright/reversed
     * 일반 해석과 내용이 겹쳐서 뺀다. */
    private static final Topic[] DETAIL_TOPICS = {Topic.LOVE, Topic.CAREER, Topic.WEALTH};

    private final CardJpaRepository cardJpaRepository;
    private final CardInterpretationJpaRepository cardInterpretationJpaRepository;
    private final CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository;

    public CardService(CardJpaRepository cardJpaRepository,
                       CardInterpretationJpaRepository cardInterpretationJpaRepository,
                       CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
        this.cardInterpretationJpaRepository = cardInterpretationJpaRepository;
        this.cardTopicInterpretationJpaRepository = cardTopicInterpretationJpaRepository;
    }

    @Transactional(readOnly = true) // 조회 전용 — 더티 체킹 스냅샷을 안 만들어서 약간 더 가볍다
    public List<CardEntity> getCards() {
        return cardJpaRepository.findAll();
    }

    /**
     * 캐시 미스면 그 자리에서 조합+저장까지 한다(getOrGenerate) — /card/[slug]는 정적 export
     * 빌드 타임에 78장 전부를 훑기 때문에, 아직 아무도 안 뽑아본 카드/방향/주제 조합도 이
     * 시점에 전부 채워져야 SEO 페이지에 빈 내용이 뜨지 않는다.
     */
    @Transactional
    public CardDetail getCard(Short cardId) {
        CardEntity card = cardJpaRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));

        String upright = getOrGenerateCardInterpretation(card, false);
        String reversed = getOrGenerateCardInterpretation(card, true);
        Map<Topic, String> uprightTopics = getOrGenerateTopicInterpretations(card, false);
        Map<Topic, String> reversedTopics = getOrGenerateTopicInterpretations(card, true);

        return new CardDetail(card, upright, reversed, uprightTopics, reversedTopics);
    }

    private Map<Topic, String> getOrGenerateTopicInterpretations(CardEntity card, boolean reversed) {
        Map<Topic, String> result = new EnumMap<>(Topic.class);
        for (Topic topic : DETAIL_TOPICS) {
            result.put(topic, getOrGenerateTopicInterpretation(card, reversed, topic));
        }
        return result;
    }

    /** 캐시 조회, 미스면 조합+저장까지 한 번에. ChemiService의 동명 메서드와 같은 패턴이지만
     * 카드 일반 해석(card_interpretations)용이라 별도로 둔다. */
    private String getOrGenerateCardInterpretation(CardEntity card, boolean reversed) {
        return cardInterpretationJpaRepository.findByCardIdAndReversed(card.getId(), reversed)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElseGet(() -> {
                    String text = CardInterpretationComposer.compose(card, reversed);
                    try {
                        cardInterpretationJpaRepository.save(new CardInterpretationEntity(card.getId(), reversed, text));
                    } catch (DataIntegrityViolationException e) {
                        // 동시에 처음 요청된 경합 — 먼저 저장된 캐시를 그대로 쓴다.
                        return cardInterpretationJpaRepository.findByCardIdAndReversed(card.getId(), reversed)
                                .map(CardInterpretationEntity::getInterpretationText)
                                .orElseThrow(() -> e);
                    }
                    return text;
                });
    }

    /** 캐시 조회, 미스면 조합+저장까지 한 번에(주제별 · card_topic_interpretations). */
    private String getOrGenerateTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                .map(CardTopicInterpretationEntity::getInterpretationText)
                .orElseGet(() -> {
                    String text = CardTopicInterpretationComposer.compose(card, reversed, topic);
                    try {
                        cardTopicInterpretationJpaRepository.save(
                                new CardTopicInterpretationEntity(card.getId(), reversed, topic, text));
                    } catch (DataIntegrityViolationException e) {
                        return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                                .map(CardTopicInterpretationEntity::getInterpretationText)
                                .orElseThrow(() -> e);
                    }
                    return text;
                });
    }
}
