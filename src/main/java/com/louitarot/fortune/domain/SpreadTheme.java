package com.louitarot.fortune.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 개인 카드 뽑기 3장의 자리 라벨 세트 ([[02 주요 기능 정의]]의 "스프레드 테마").
 * "과거/현재/미래"로 고정하지 않고 KST 기준 날짜로 매일 자동 회전한다 — 같은 날엔 모두에게
 * 같은 테마가 노출된다(1월 1일부터 누적 일수 % 테마 개수).
 *
 * key는 fortune_draws.spread_theme_key에 저장된다. 라벨 텍스트가 아니라 key를 저장하는 이유:
 * 테마 목록이 나중에 늘어나거나 문구가 바뀌어도 이미 만들어진 결과의 라벨은 그대로 유지돼야 하기
 * 때문 — fromKey(key)로 그 draw가 뽑힌 시점의 라벨을 언제든 다시 계산할 수 있다.
 */
public enum SpreadTheme {
    TIME("time", List.of("과거", "현재", "미래")),
    EMOTION("emotion", List.of("감정", "행동", "이해")),
    BALANCE("balance", List.of("내면", "외면", "내면과 외면의 균형")),
    CAUSE_RESULT("cause", List.of("원인", "경과", "결과")),
    GROWTH("growth", List.of("두려움", "극복", "성장")),
    RELATION("relation", List.of("나", "관계", "세상")),
    RELEASE("release", List.of("놓아야 할 것", "붙잡아야 할 것", "나아갈 방향")),
    TODAY("today", List.of("오늘의 나", "오늘의 장애물", "오늘의 조언")),
    SEED("seed", List.of("씨앗", "뿌리", "열매")),
    INSIGHT("insight", List.of("질문", "응답", "깨달음"));

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final String key;
    private final List<String> positionLabels;

    SpreadTheme(String key, List<String> positionLabels) {
        this.key = key;
        this.positionLabels = positionLabels;
    }

    public String key() {
        return key;
    }

    public String labelAt(int positionIndex) {
        return positionLabels.get(positionIndex);
    }

    /** KST 기준 오늘의 테마. */
    public static SpreadTheme resolveForToday() {
        int dayOfYear = LocalDate.now(KST).getDayOfYear();
        SpreadTheme[] values = values();
        return values[(dayOfYear - 1) % values.length];
    }

    /** 이미 저장된 draw를 다시 보여줄 때, 뽑힌 시점의 테마를 key로 복원한다. */
    public static SpreadTheme fromKey(String key) {
        for (SpreadTheme theme : values()) {
            if (theme.key.equals(key)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("알 수 없는 스프레드 테마 키: " + key);
    }
}
