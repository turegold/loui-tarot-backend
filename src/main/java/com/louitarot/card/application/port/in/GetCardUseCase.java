package com.louitarot.card.application.port.in;

import com.louitarot.card.application.CardDetail;

/** [[API 명세]]의 GET /cards/{cardId} — 카드 상세 + 정/역방향 범용 해석. */
public interface GetCardUseCase {

    /** cardId에 해당하는 카드가 없으면 CustomException(ErrorCode.CARD_NOT_FOUND)를 던진다. */
    CardDetail getCard(Short cardId);
}
