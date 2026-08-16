package com.louitarot.chemi.service;

import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.Element;
import com.louitarot.card.entity.CardEntity;

/**
 * [[케미 점수 산출 로직]]을 그대로 옮긴 순수 함수. AI가 아니라 카드 속성 기반 규칙으로
 * 결정론적으로 점수를 낸다 — 같은 조합이면 항상 같은 점수가 나와야 캐싱/재현성이 성립한다.
 */
public final class ChemiScoreCalculator {

    private static final int BASE_SCORE = 50;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private ChemiScoreCalculator() {
    }

    public static short calculate(CardEntity cardA, boolean reversedA, CardEntity cardB, boolean reversedB) {
        int score = BASE_SCORE
                + elementScore(cardA, cardB)
                + arcanaScore(cardA, cardB)
                + directionScore(reversedA, reversedB);
        return (short) Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    /** 마이너 아르카나끼리만 적용된다. 한쪽이라도 메이저면 원소 자체가 없어 이 축은 기여하지 않는다. */
    private static int elementScore(CardEntity cardA, CardEntity cardB) {
        Element a = cardA.getElement();
        Element b = cardB.getElement();
        if (a == null || b == null) {
            return 0;
        }
        if (a == b) {
            return 15; // 동일 원소
        }
        if (isFriendly(a, b)) {
            return 20; // 우호 (불-공, 물-흙)
        }
        if (isHostile(a, b)) {
            return -20; // 상극 (불-물, 공-흙)
        }
        return 0; // 중립 (불-흙, 물-공)
    }

    private static boolean isFriendly(Element a, Element b) {
        return isPair(a, b, Element.FIRE, Element.AIR) || isPair(a, b, Element.WATER, Element.EARTH);
    }

    private static boolean isHostile(Element a, Element b) {
        return isPair(a, b, Element.FIRE, Element.WATER) || isPair(a, b, Element.AIR, Element.EARTH);
    }

    private static boolean isPair(Element a, Element b, Element x, Element y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    private static int arcanaScore(CardEntity cardA, CardEntity cardB) {
        boolean majorA = cardA.getArcanaType() == ArcanaType.MAJOR;
        boolean majorB = cardB.getArcanaType() == ArcanaType.MAJOR;
        if (majorA && majorB) {
            return 10;
        }
        if (majorA || majorB) {
            return 5;
        }
        return 0;
    }

    private static int directionScore(boolean reversedA, boolean reversedB) {
        if (!reversedA && !reversedB) {
            return 15;
        }
        if (reversedA && reversedB) {
            return -10;
        }
        return -5; // 정+역
    }
}
