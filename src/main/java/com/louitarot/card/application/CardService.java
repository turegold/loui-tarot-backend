package com.louitarot.card.application;

import com.louitarot.card.adapter.out.persistence.CardEntity;
import com.louitarot.card.adapter.out.persistence.CardInterpretationEntity;
import com.louitarot.card.application.port.in.GetCardUseCase;
import com.louitarot.card.application.port.in.GetCardsUseCase;
import com.louitarot.card.application.port.out.LoadCardInterpretationPort;
import com.louitarot.card.application.port.out.LoadCardPort;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카드 조회 유스케이스 2개의 구현체. 인바운드 포트(GetCardsUseCase/GetCardUseCase)를 구현하고,
 * 아웃바운드 포트(LoadCardPort/LoadCardInterpretationPort)에만 의존한다 — JPA도 스프링 웹도
 * 이 클래스 시그니처에는 등장하지 않는다(트랜잭션 애너테이션 정도만 예외).
 */
@Service
public class CardService implements GetCardsUseCase, GetCardUseCase {

    private final LoadCardPort loadCardPort;
    private final LoadCardInterpretationPort loadCardInterpretationPort;

    public CardService(LoadCardPort loadCardPort, LoadCardInterpretationPort loadCardInterpretationPort) {
        this.loadCardPort = loadCardPort;
        this.loadCardInterpretationPort = loadCardInterpretationPort;
    }

    @Override
    @Transactional(readOnly = true) // 조회 전용 — 더티 체킹 스냅샷을 안 만들어서 약간 더 가볍다
    public List<CardEntity> getCards() {
        return loadCardPort.loadAllCards();
    }

    @Override
    @Transactional(readOnly = true)
    public CardDetail getCard(Short cardId) {
        CardEntity card = loadCardPort.loadCard(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));

        String upright = loadCardInterpretationPort.loadInterpretation(cardId, false)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElse(null);
        String reversed = loadCardInterpretationPort.loadInterpretation(cardId, true)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElse(null);

        return new CardDetail(card, upright, reversed);
    }
}
