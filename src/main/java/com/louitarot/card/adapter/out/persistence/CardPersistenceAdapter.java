package com.louitarot.card.adapter.out.persistence;

import com.louitarot.card.application.port.out.LoadCardInterpretationPort;
import com.louitarot.card.application.port.out.LoadCardPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * LoadCardPort/LoadCardInterpretationPort의 실제 구현체 — JPA로 두 포트를 채운다.
 * 두 포트를 한 클래스가 같이 구현한 이유는 "카드 관련 데이터를 영속성 계층에서 읽어온다"는
 * 하나의 책임으로 묶이기 때문. 인터페이스는 목적별로 분리하고, 구현은 하나로 묶은 것.
 */
@Component
public class CardPersistenceAdapter implements LoadCardPort, LoadCardInterpretationPort {

    private final CardJpaRepository cardJpaRepository;
    private final CardInterpretationJpaRepository cardInterpretationJpaRepository;

    public CardPersistenceAdapter(CardJpaRepository cardJpaRepository,
                                  CardInterpretationJpaRepository cardInterpretationJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
        this.cardInterpretationJpaRepository = cardInterpretationJpaRepository;
    }

    @Override
    public List<CardEntity> loadAllCards() {
        return cardJpaRepository.findAll();
    }

    @Override
    public Optional<CardEntity> loadCard(Short cardId) {
        return cardJpaRepository.findById(cardId);
    }

    @Override
    public Optional<CardInterpretationEntity> loadInterpretation(Short cardId, boolean reversed) {
        return cardInterpretationJpaRepository.findByCardIdAndReversed(cardId, reversed);
    }
}
