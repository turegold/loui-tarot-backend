package com.louitarot.card.service;

import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.entity.CardInterpretationEntity;
import com.louitarot.card.repository.CardInterpretationJpaRepository;
import com.louitarot.card.repository.CardJpaRepository;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카드 조회 서비스. 일반적인 레이어드 스타일(Controller → Service → Repository)로 짠다 —
 * Spring Data Repository 자체가 이미 인터페이스라 그 위에 포트를 한 겹 더 두는 게 실익이 없어서,
 * DB 접근에는 헥사고날 포트/어댑터를 적용하지 않기로 했다. 포트는 AI API·카카오 로그인처럼
 * "원래 인터페이스가 없던 자리"에만 적용한다 ([[백엔드 설계 원칙]] 1번).
 */
@Service
public class CardService {

    private final CardJpaRepository cardJpaRepository;
    private final CardInterpretationJpaRepository cardInterpretationJpaRepository;

    public CardService(CardJpaRepository cardJpaRepository,
                       CardInterpretationJpaRepository cardInterpretationJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
        this.cardInterpretationJpaRepository = cardInterpretationJpaRepository;
    }

    @Transactional(readOnly = true) // 조회 전용 — 더티 체킹 스냅샷을 안 만들어서 약간 더 가볍다
    public List<CardEntity> getCards() {
        return cardJpaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CardDetail getCard(Short cardId) {
        CardEntity card = cardJpaRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));

        String upright = cardInterpretationJpaRepository.findByCardIdAndReversed(cardId, false)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElse(null);
        String reversed = cardInterpretationJpaRepository.findByCardIdAndReversed(cardId, true)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElse(null);

        return new CardDetail(card, upright, reversed);
    }
}
