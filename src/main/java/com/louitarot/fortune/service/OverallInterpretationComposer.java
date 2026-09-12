package com.louitarot.fortune.service;

import com.louitarot.card.domain.CardEssence;
import com.louitarot.card.domain.CardEssenceCatalog;
import com.louitarot.card.domain.CardLeaning;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.common.domain.Topic;
import com.louitarot.fortune.domain.SpreadTheme;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3장 종합 해석을 AI 호출 없이 카드 속성 기반 문구 조합으로 만드는 결정론적 규칙 + 무작위
 * 문구 선택 조합. ChemiScoreCalculator와 같은 이유로 순수 유틸 클래스(포트로 감싸지 않음) —
 * 원래 인터페이스가 없던 외부 SDK 자리가 아니라 인프로세스 로직이라 [[백엔드 설계 원칙]] 1번의
 * 헥사고날 적용 범위 밖이다.
 *
 * 3장 조합의 경우의 수(370만+)가 캐싱은 물론 매 요청 AI 호출도 부담스러워([[AI 해석 캐싱 전략]])
 * AI 대신 이 클래스로 대체했다 — 문구 선택은 매 호출 무작위라 재현성이 없지만,
 * 생성 결과가 fortune_draws.overall_interpretation_text에 한 번 저장되고 이후 재계산되지
 * 않으므로 문제되지 않는다. 카드별 키워드/기운/조언은 CardEssenceCatalog(케미 쪽 해석과 공용)에서 가져온다.
 */
public final class OverallInterpretationComposer {

    private OverallInterpretationComposer() {
    }

    private enum Mood { RISING, CAUTION, MIXED }

    private static final Map<Topic, String[]> OPENINGS = Map.of(
            Topic.COMPREHENSIVE, new String[]{
                    "오늘의 종합운을 세 장의 카드로 짚어봤어요.",
                    "지금 흐름이 어떤지 카드가 이렇게 말해주네요.",
                    "세 장의 카드가 지금 상황을 이렇게 그려내고 있어요."},
            Topic.LOVE, new String[]{
                    "연애운의 흐름을 세 장의 카드로 살펴봤어요.",
                    "마음의 방향을 카드가 이렇게 짚어주네요.",
                    "관계 속 흐름이 이 세 장에 담겨 있어요."},
            Topic.CAREER, new String[]{
                    "취업운의 흐름을 세 장의 카드로 확인해봤어요.",
                    "지금 나아갈 방향을 카드가 이렇게 보여주네요.",
                    "커리어의 흐름이 이 세 장에 담겨 있어요."},
            Topic.WEALTH, new String[]{
                    "재물운의 흐름을 세 장의 카드로 짚어봤어요.",
                    "돈의 흐름을 카드가 이렇게 말해주네요.",
                    "재물과 관련된 기운이 이 세 장에 담겨 있어요."});

    private static final Map<Mood, String[]> CONNECTORS = Map.of(
            Mood.RISING, new String[]{"그리고 이어서", "여기서 한 걸음 더 나아가", "점점 힘을 얻어", "자연스럽게 이어지며", "탄력을 받아"},
            Mood.CAUTION, new String[]{"하지만 이어서", "그런데 여기서", "조심스럽게 이어지며", "쉽지 않게도", "고비를 지나며"},
            Mood.MIXED, new String[]{"그런가 하면", "이어서는", "한편으로는", "다른 한편", "흐름이 바뀌며"});

    private static final Map<Mood, String[]> CLOSINGS = Map.of(
            Mood.RISING, new String[]{
                    "전체적으로 순조롭게 풀려가는 흐름이니 지금의 기세를 이어가 보세요.",
                    "좋은 기운이 이어지고 있으니 자신 있게 나아가도 좋겠어요.",
                    "차근차근 원하는 방향으로 다가가고 있는 흐름이에요."},
            Mood.CAUTION, new String[]{
                    "조금 조심스러운 흐름이니 서두르지 말고 하나씩 짚어가 보세요.",
                    "지금은 무리하기보다 잠시 숨을 고르는 게 필요한 때예요.",
                    "쉽지 않은 흐름이지만 차분히 대응하면 곧 나아질 거예요."},
            Mood.MIXED, new String[]{
                    "좋은 기운과 조심할 기운이 함께 있으니 균형 있게 바라보세요.",
                    "상황이 엇갈리는 만큼 유연하게 대처하는 게 도움이 될 거예요.",
                    "흐름이 오르내리는 지금, 무리하지 않는 선에서 판단해보세요."});

    public static String compose(Topic topic, SpreadTheme theme, List<CardEntity> cards, List<Boolean> reversedFlags) {
        CardEssence first = CardEssenceCatalog.resolve(cards.get(0), reversedFlags.get(0));
        CardEssence second = CardEssenceCatalog.resolve(cards.get(1), reversedFlags.get(1));
        CardEssence third = CardEssenceCatalog.resolve(cards.get(2), reversedFlags.get(2));
        Mood mood = classify(first.leaning(), second.leaning(), third.leaning());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        String opening = pickOne(OPENINGS.get(topic), random);
        String[] connectorPool = CONNECTORS.get(mood);
        String connector1 = pickOne(connectorPool, random);
        String connector2 = pickOneExcluding(connectorPool, connector1, random);
        String closing = pickOne(CLOSINGS.get(mood), random);

        return """
                %s %s에서는 %s, %s %s에서는 %s, %s %s에서는 %s의 기운이 보여요. \
                %s 특히 %s 자리를 보면, %s.\
                """.formatted(
                opening,
                theme.labelAt(0), first.keyword(),
                connector1, theme.labelAt(1), second.keyword(),
                connector2, theme.labelAt(2), third.keyword(),
                closing, theme.labelAt(2), third.advice());
    }

    private static Mood classify(CardLeaning first, CardLeaning second, CardLeaning third) {
        long caution = List.of(first, second, third).stream().filter(l -> l == CardLeaning.CAUTION).count();
        long positive = List.of(first, second, third).stream().filter(l -> l == CardLeaning.POSITIVE).count();
        if (caution >= 2) {
            return Mood.CAUTION;
        }
        if (positive >= 2) {
            return Mood.RISING;
        }
        return Mood.MIXED;
    }

    private static String pickOne(String[] pool, ThreadLocalRandom random) {
        return pool[random.nextInt(pool.length)];
    }

    private static String pickOneExcluding(String[] pool, String exclude, ThreadLocalRandom random) {
        String picked;
        do {
            picked = pool[random.nextInt(pool.length)];
        } while (picked.equals(exclude) && pool.length > 1);
        return picked;
    }
}
