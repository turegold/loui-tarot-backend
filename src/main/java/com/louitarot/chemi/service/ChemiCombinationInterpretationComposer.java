package com.louitarot.chemi.service;

import com.louitarot.card.domain.CardEssence;
import com.louitarot.card.domain.CardEssenceCatalog;
import com.louitarot.card.entity.CardEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 케미 조합 해석을 AI 호출 없이 즉시 조합한다. chemi_combinations 캐시가
 * (카드A, 방향A, 카드B, 방향B) 조합당 한 번만 저장하므로, 여기서 무작위로 고른 문구가
 * 그대로 영구 캐싱된다 — [[AI 해석 캐싱 전략]]. 점수(ChemiScoreCalculator 결과) 구간에 따라
 * 문구 풀만 다르게 고른다.
 */
public final class ChemiCombinationInterpretationComposer {

    private ChemiCombinationInterpretationComposer() {
    }

    private enum Tier { HIGH, MID, LOW }

    private static final String[] HIGH_OPENINGS = {
            "아주 잘 어울리는 조합이에요.", "서로를 자연스럽게 채워주는 케미예요.", "함께 있을 때 시너지가 좋은 조합이에요."};
    private static final String[] MID_OPENINGS = {
            "무난하게 어울리는 조합이에요.", "특별히 튀지는 않지만 안정적인 케미예요.", "서로 부딪히지 않고 잘 지낼 수 있는 조합이에요."};
    private static final String[] LOW_OPENINGS = {
            "서로 다른 결을 가진 조합이에요.", "맞춰가려면 약간의 노력이 필요한 케미예요.", "쉽게 스며들기보다 시간이 필요한 조합이에요."};

    private static final String[] HIGH_CLOSINGS = {
            "지금 이대로도 충분히 좋은 케미예요.", "이 흐름이라면 앞으로도 잘 맞아갈 거예요.", "서로에게 좋은 영향을 주는 관계예요."};
    private static final String[] MID_CLOSINGS = {
            "적당한 거리감이 오히려 편안함을 줄 수 있어요.", "무리하지 않아도 자연스럽게 이어질 케미예요.", "큰 기복 없이 무난하게 흘러갈 거예요."};
    private static final String[] LOW_CLOSINGS = {
            "서로 다른 점을 인정하면 오히려 배울 게 많을 거예요.", "천천히 맞춰간다면 의외의 케미가 생길 수 있어요.", "다름을 이해하려는 노력이 관계를 더 단단하게 만들어줄 거예요."};

    public static String compose(CardEntity cardA, boolean reversedA, CardEntity cardB, boolean reversedB, short score) {
        CardEssence essenceA = CardEssenceCatalog.resolve(cardA, reversedA);
        CardEssence essenceB = CardEssenceCatalog.resolve(cardB, reversedB);
        Tier tier = tierOf(score);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String opening = pick(openingsOf(tier), random);
        String closing = pick(closingsOf(tier), random);

        // 카드 이름(마이너는 "소드 3"처럼 숫자로 끝나기도 해서 받침 판정이 애매하다)에는 조사를 직접
        // 붙이지 않고 "두 카드가"로 고정 — 대신 항상 완성형 한글로만 이뤄진 키워드 문구(essence)에는
        // 받침 유무에 따라 와/과를 정확히 골라 붙인다.
        return """
                '%s'(%s), '%s'(%s) 두 카드가 만났어요. 궁합 점수는 %d점으로 '%s' 정도예요. \
                %s 이번 만남에서는 %s%s %s의 기운이 함께 흐르고 있어요. %s\
                """.formatted(
                cardA.getNameKr(), reversedA ? "역방향" : "정방향",
                cardB.getNameKr(), reversedB ? "역방향" : "정방향",
                score, toneLabel(score),
                opening, essenceA.keyword(), josaWaGwa(essenceA.keyword()), essenceB.keyword(), closing);
    }

    /** 완성형 한글 음절의 받침 유무로 와/과를 고른다 — 받침 있으면 "과", 없으면 "와". */
    private static String josaWaGwa(String word) {
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return "와";
        }
        return (last - 0xAC00) % 28 == 0 ? "와" : "과";
    }

    /** ChemiService에 있던 표시용 점수 등급 — 5단계 라벨은 그대로 두고, 문구 풀 선택만 3단계로 단순화했다. */
    private static String toneLabel(short score) {
        if (score >= 90) {
            return "천생연분";
        }
        if (score >= 70) {
            return "잘 맞음";
        }
        if (score >= 50) {
            return "무난";
        }
        if (score >= 30) {
            return "노력 필요";
        }
        return "상극";
    }

    private static Tier tierOf(short score) {
        if (score >= 70) {
            return Tier.HIGH;
        }
        if (score >= 50) {
            return Tier.MID;
        }
        return Tier.LOW;
    }

    private static String[] openingsOf(Tier tier) {
        return switch (tier) {
            case HIGH -> HIGH_OPENINGS;
            case MID -> MID_OPENINGS;
            case LOW -> LOW_OPENINGS;
        };
    }

    private static String[] closingsOf(Tier tier) {
        return switch (tier) {
            case HIGH -> HIGH_CLOSINGS;
            case MID -> MID_CLOSINGS;
            case LOW -> LOW_CLOSINGS;
        };
    }

    private static String pick(String[] pool, ThreadLocalRandom random) {
        return pool[random.nextInt(pool.length)];
    }
}
