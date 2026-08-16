package com.louitarot.chemi.service;

import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.Element;
import com.louitarot.card.domain.Suit;
import com.louitarot.card.entity.CardEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** [[케미 점수 산출 로직]] 각 축(원소/아르카나/방향)이 규칙대로 합산되는지 검증. */
class ChemiScoreCalculatorTest {

    private static CardEntity major(short id) {
        return new CardEntity(id, "메이저" + id, "Major" + id, ArcanaType.MAJOR, null, null, id, "url", "slug" + id);
    }

    private static CardEntity minor(short id, Suit suit, Element element) {
        return new CardEntity(id, "마이너" + id, "Minor" + id, ArcanaType.MINOR, suit, element, (short) 1, "url", "slug" + id);
    }

    @Test
    void 메이저_둘_다_정방향이면_기본50_아르카나10_방향15() {
        CardEntity a = major((short) 1);
        CardEntity b = major((short) 2);

        short score = ChemiScoreCalculator.calculate(a, false, b, false);

        assertEquals(75, score); // 50 + 10(메이저+메이저) + 0(원소 없음) + 15(정+정)
    }

    @Test
    void 동일_원소는_15점_가점() {
        CardEntity a = minor((short) 1, Suit.WAND, Element.FIRE);
        CardEntity b = minor((short) 2, Suit.WAND, Element.FIRE);

        short score = ChemiScoreCalculator.calculate(a, false, b, false);

        assertEquals(80, score); // 50 + 15(동일 원소) + 0(마이너+마이너) + 15(정+정)
    }

    @Test
    void 우호_원소는_20점_가점() {
        CardEntity fire = minor((short) 1, Suit.WAND, Element.FIRE);
        CardEntity air = minor((short) 2, Suit.SWORD, Element.AIR);

        short score = ChemiScoreCalculator.calculate(fire, false, air, false);

        assertEquals(85, score); // 50 + 20(우호) + 0 + 15
    }

    @Test
    void 상극_원소는_20점_감점() {
        CardEntity fire = minor((short) 1, Suit.WAND, Element.FIRE);
        CardEntity water = minor((short) 2, Suit.CUP, Element.WATER);

        short score = ChemiScoreCalculator.calculate(fire, false, water, false);

        assertEquals(45, score); // 50 - 20(상극) + 0 + 15
    }

    @Test
    void 중립_원소는_가감점_없음() {
        CardEntity fire = minor((short) 1, Suit.WAND, Element.FIRE);
        CardEntity earth = minor((short) 2, Suit.PENTACLE, Element.EARTH);

        short score = ChemiScoreCalculator.calculate(fire, false, earth, false);

        assertEquals(65, score); // 50 + 0(중립) + 0 + 15
    }

    @Test
    void 메이저_마이너_조합은_5점_가점() {
        CardEntity majorCard = major((short) 1);
        CardEntity minorCard = minor((short) 2, Suit.CUP, Element.WATER);

        short score = ChemiScoreCalculator.calculate(majorCard, false, minorCard, false);

        assertEquals(70, score); // 50 + 0(원소 없음, 한쪽 메이저) + 5(메이저+마이너) + 15(정+정)
    }

    @Test
    void 정역_조합은_5점_감점() {
        CardEntity a = major((short) 1);
        CardEntity b = major((short) 2);

        short score = ChemiScoreCalculator.calculate(a, false, b, true);

        assertEquals(55, score); // 50 + 10 + 0 - 5(정+역)
    }

    @Test
    void 역방향_둘_다면_10점_감점() {
        CardEntity a = major((short) 1);
        CardEntity b = major((short) 2);

        short score = ChemiScoreCalculator.calculate(a, true, b, true);

        assertEquals(50, score); // 50 + 10 + 0 - 10(역+역)
    }

    @Test
    void 카드_순서를_바꿔도_점수는_동일() {
        CardEntity fire = minor((short) 1, Suit.WAND, Element.FIRE);
        CardEntity water = minor((short) 2, Suit.CUP, Element.WATER);

        short score1 = ChemiScoreCalculator.calculate(fire, false, water, true);
        short score2 = ChemiScoreCalculator.calculate(water, true, fire, false);

        assertEquals(score1, score2);
    }
}
