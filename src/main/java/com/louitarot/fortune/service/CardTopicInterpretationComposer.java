package com.louitarot.fortune.service;

import com.louitarot.card.domain.CardEssence;
import com.louitarot.card.domain.CardEssenceCatalog;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.common.domain.Topic;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 개인 카드 뽑기용 카드 한 장 + 주제 해석을 AI 호출 없이 즉시 조합한다.
 * card_topic_interpretations 캐시가 (카드, 방향, 주제) 조합당 한 번만 저장하므로, 여기서
 * 무작위로 고른 문구가 그대로 영구 캐싱된다 — [[AI 해석 캐싱 전략]].
 */
public final class CardTopicInterpretationComposer {

    private CardTopicInterpretationComposer() {
    }

    private static final Map<Topic, String[]> OPENINGS = Map.of(
            Topic.COMPREHENSIVE, new String[]{"지금 전반적인 흐름에서는", "요즘 전체적인 상황을 보면", "지금 흐름을 종합해서 보면"},
            Topic.LOVE, new String[]{"연애 쪽 흐름에서는", "마음이 향하는 방향을 보면", "관계의 흐름을 보면"},
            Topic.CAREER, new String[]{"취업 쪽 흐름에서는", "커리어 방향을 보면", "일과 관련해서는"},
            Topic.WEALTH, new String[]{"재물 쪽 흐름에서는", "돈과 관련된 흐름을 보면", "재정적인 부분을 보면"});

    public static String compose(CardEntity card, boolean reversed, Topic topic) {
        CardEssence essence = CardEssenceCatalog.resolve(card, reversed);
        String[] pool = OPENINGS.get(topic);
        String opening = pool[ThreadLocalRandom.current().nextInt(pool.length)];
        return "'%s' 카드가 %s으로 나왔어요. %s %s의 기운이 느껴져요. %s."
                .formatted(card.getNameKr(), reversed ? "역방향" : "정방향", opening, essence.keyword(), essence.advice());
    }
}
