package com.louitarot.chemi.service;

import com.louitarot.card.domain.CardEssence;
import com.louitarot.card.domain.CardEssenceCatalog;
import com.louitarot.card.entity.CardEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 케미 뽑기용 카드 한 장 해석(주제 없음)을 AI 호출 없이 즉시 조합한다.
 * card_interpretations 캐시가 (카드, 방향) 조합당 한 번만 저장하므로, 여기서 무작위로 고른
 * 문구가 그대로 영구 캐싱된다 — [[AI 해석 캐싱 전략]].
 */
public final class CardInterpretationComposer {

    private CardInterpretationComposer() {
    }

    private static final String[] OPENINGS = {"지금은", "이 카드가 말해주는 건", "가만히 보면", "이 순간에는"};

    public static String compose(CardEntity card, boolean reversed) {
        CardEssence essence = CardEssenceCatalog.resolve(card, reversed);
        String opening = OPENINGS[ThreadLocalRandom.current().nextInt(OPENINGS.length)];
        return "'%s' 카드가 %s으로 나왔어요. %s %s의 기운이 느껴져요. %s."
                .formatted(card.getNameKr(), reversed ? "역방향" : "정방향", opening, essence.keyword(), essence.advice());
    }
}
