package com.louitarot.card.domain;

import com.louitarot.card.entity.CardEntity;

import java.util.Map;

/**
 * 78장 카드(정/역 포함 156가지)의 짧은 해석 조각(키워드/기운/조언) 카탈로그.
 * AI 호출 없이 템플릿으로 해석 문장을 조합하는 모든 곳(케미 카드/조합 해석, 개인 카드 뽑기
 * 카드별·종합 해석)이 공통으로 참조하는 카드 도메인 지식이다 — [[AI 해석 캐싱 전략]].
 *
 * 메이저 아르카나(22장)는 카드 이름별로, 마이너 아르카나(56장)는 수트 단위로 묶었다 —
 * 마이너까지 카드 한 장 한 장 다르게 쓰는 건 지금 필요한 것보다 과한 작업이라
 * ([[백엔드 설계 원칙]] 8번, 조기 최적화 금지) 수트 4종 × 정역 2종(8가지)으로 단순화했다.
 * 같은 수트의 마이너 카드끼리는 문구가 겹칠 수 있다는 뜻 — 피드백을 보고 필요하면 랭크
 * (에이스/코트 등) 단위로 세분화한다.
 */
public final class CardEssenceCatalog {

    private CardEssenceCatalog() {
    }

    /** 이름 매칭이 어긋나는 경우(오타 등) 대비용 — 해석 생성 전체가 NPE로 죽는 것보다 안전하다. */
    private static final CardEssence FALLBACK =
            new CardEssence("신비로운 기운", CardLeaning.NEUTRAL, "카드가 전하는 느낌에 집중해보세요");

    private static final Map<String, CardEssence> MAJOR_UPRIGHT = Map.ofEntries(
            Map.entry("바보", new CardEssence("새로운 시작", CardLeaning.POSITIVE, "일단 가볍게 첫걸음을 내디뎌보세요")),
            Map.entry("마법사", new CardEssence("가능성의 실현", CardLeaning.POSITIVE, "가진 능력을 지금 바로 활용해보세요")),
            Map.entry("여사제", new CardEssence("내면의 직관", CardLeaning.NEUTRAL, "논리보다 직감이 하는 말에 귀 기울여보세요")),
            Map.entry("여황제", new CardEssence("풍요와 돌봄", CardLeaning.POSITIVE, "스스로를 돌보는 데도 마음을 써보세요")),
            Map.entry("황제", new CardEssence("안정과 통제", CardLeaning.POSITIVE, "원칙을 세우고 그대로 밀고 나가보세요")),
            Map.entry("교황", new CardEssence("전통과 조언", CardLeaning.NEUTRAL, "믿을 만한 사람의 조언을 구해보세요")),
            Map.entry("연인", new CardEssence("조화로운 선택", CardLeaning.POSITIVE, "마음이 이끄는 방향을 믿어보세요")),
            Map.entry("전차", new CardEssence("의지와 전진", CardLeaning.POSITIVE, "지금의 추진력을 밀고 나가보세요")),
            Map.entry("힘", new CardEssence("부드러운 용기", CardLeaning.POSITIVE, "강하게 맞서기보다 부드럽게 다뤄보세요")),
            Map.entry("은둔자", new CardEssence("홀로 있는 성찰", CardLeaning.NEUTRAL, "잠시 혼자만의 시간을 가져보세요")),
            Map.entry("운명의 수레바퀴", new CardEssence("변화의 흐름", CardLeaning.POSITIVE, "흐름이 바뀌는 지금을 놓치지 마세요")),
            Map.entry("정의", new CardEssence("공정한 결과", CardLeaning.POSITIVE, "원칙대로 판단하면 결과도 따라올 거예요")),
            Map.entry("매달린 사람", new CardEssence("기다림의 지혜", CardLeaning.NEUTRAL, "지금은 억지로 움직이기보다 기다려보세요")),
            Map.entry("죽음", new CardEssence("끝과 새로운 국면", CardLeaning.NEUTRAL, "끝나야 할 것은 붙잡지 말고 놓아주세요")),
            Map.entry("절제", new CardEssence("균형과 조화", CardLeaning.POSITIVE, "서두르지 말고 균형을 맞춰가 보세요")),
            Map.entry("악마", new CardEssence("집착과 속박", CardLeaning.CAUTION, "나를 옭아매는 것에서 벗어날 방법을 찾아보세요")),
            Map.entry("탑", new CardEssence("갑작스러운 붕괴", CardLeaning.CAUTION, "무너진 자리에서 다시 세울 것을 고민해보세요")),
            Map.entry("별", new CardEssence("희망과 치유", CardLeaning.POSITIVE, "지금의 희망을 붙잡고 나아가 보세요")),
            Map.entry("달", new CardEssence("불안과 모호함", CardLeaning.CAUTION, "불확실함 속에서도 직감을 믿어보세요")),
            Map.entry("태양", new CardEssence("성취와 밝은 기운", CardLeaning.POSITIVE, "지금의 좋은 기운을 마음껏 누려보세요")),
            Map.entry("심판", new CardEssence("각성과 재평가", CardLeaning.POSITIVE, "지난 시간을 돌아보고 새롭게 결단해보세요")),
            Map.entry("세계", new CardEssence("완성과 성취", CardLeaning.POSITIVE, "한 단계를 완성한 자신을 인정해주세요")));

    private static final Map<String, CardEssence> MAJOR_REVERSED = Map.ofEntries(
            Map.entry("바보", new CardEssence("무모한 선택", CardLeaning.CAUTION, "속도를 늦추고 준비를 다시 점검해보세요")),
            Map.entry("마법사", new CardEssence("능력의 공회전", CardLeaning.CAUTION, "계획만 세우지 말고 실행으로 옮겨보세요")),
            Map.entry("여사제", new CardEssence("숨겨진 진실", CardLeaning.CAUTION, "드러나지 않은 부분을 성급히 판단하지 마세요")),
            Map.entry("여황제", new CardEssence("과잉과 소진", CardLeaning.CAUTION, "너무 애쓰고 있진 않은지 점검해보세요")),
            Map.entry("황제", new CardEssence("경직된 고집", CardLeaning.CAUTION, "융통성을 조금 더 허용해보세요")),
            Map.entry("교황", new CardEssence("관습에서 벗어남", CardLeaning.NEUTRAL, "정해진 방식을 벗어나도 괜찮아요")),
            Map.entry("연인", new CardEssence("갈등과 불균형", CardLeaning.CAUTION, "관계에서 어긋난 부분을 솔직히 짚어보세요")),
            Map.entry("전차", new CardEssence("방향성 상실", CardLeaning.CAUTION, "속도를 줄이고 방향부터 다시 잡아보세요")),
            Map.entry("힘", new CardEssence("자신감 부족", CardLeaning.CAUTION, "스스로를 의심하는 마음부터 다독여주세요")),
            Map.entry("은둔자", new CardEssence("고립과 단절", CardLeaning.CAUTION, "혼자 끌어안지 말고 주변에 손을 내밀어보세요")),
            Map.entry("운명의 수레바퀴", new CardEssence("예기치 못한 변수", CardLeaning.CAUTION, "계획이 어긋나도 유연하게 대응해보세요")),
            Map.entry("정의", new CardEssence("불균형과 편파", CardLeaning.CAUTION, "어느 한쪽으로 치우치진 않았는지 돌아보세요")),
            Map.entry("매달린 사람", new CardEssence("정체된 희생", CardLeaning.CAUTION, "의미 없이 버티고만 있진 않은지 살펴보세요")),
            Map.entry("죽음", new CardEssence("변화에 대한 저항", CardLeaning.CAUTION, "바뀌어야 할 것을 계속 미루고 있진 않은가요")),
            Map.entry("절제", new CardEssence("과잉과 불균형", CardLeaning.CAUTION, "한쪽으로 치우친 습관을 조정해보세요")),
            Map.entry("악마", new CardEssence("속박에서의 해방", CardLeaning.POSITIVE, "묶여 있던 것에서 벗어날 용기를 내보세요")),
            Map.entry("탑", new CardEssence("위기의 지연", CardLeaning.CAUTION, "더 늦기 전에 근본적인 문제를 마주해보세요")),
            Map.entry("별", new CardEssence("희망의 상실", CardLeaning.CAUTION, "잠깐 잃었던 희망을 다시 찾아보는 게 필요해요")),
            Map.entry("달", new CardEssence("혼란의 해소", CardLeaning.NEUTRAL, "안개가 걷히듯 상황이 점차 분명해질 거예요")),
            Map.entry("태양", new CardEssence("일시적인 그늘", CardLeaning.NEUTRAL, "잠깐의 흐림에도 곧 볕이 들 거예요")),
            Map.entry("심판", new CardEssence("미련과 자기 의심", CardLeaning.CAUTION, "스스로를 너무 몰아붙이지 마세요")),
            Map.entry("세계", new CardEssence("미완성의 아쉬움", CardLeaning.CAUTION, "마무리 짓지 못한 부분을 마저 채워보세요")));

    private static final Map<Suit, CardEssence> MINOR_UPRIGHT = Map.of(
            Suit.WAND, new CardEssence("열정적인 행동력", CardLeaning.POSITIVE, "떠오른 의욕을 지금 행동으로 옮겨보세요"),
            Suit.CUP, new CardEssence("따뜻한 감정의 흐름", CardLeaning.POSITIVE, "마음이 향하는 관계에 조금 더 다가가 보세요"),
            Suit.SWORD, new CardEssence("명확한 판단력", CardLeaning.NEUTRAL, "생각을 정리해서 필요한 결단을 내려보세요"),
            Suit.PENTACLE, new CardEssence("현실적인 안정", CardLeaning.POSITIVE, "차근차근 쌓아온 결과를 믿어보세요"));

    private static final Map<Suit, CardEssence> MINOR_REVERSED = Map.of(
            Suit.WAND, new CardEssence("지친 의욕", CardLeaning.CAUTION, "무리해서 밀어붙이기보다 페이스를 조절해보세요"),
            Suit.CUP, new CardEssence("감정의 기복", CardLeaning.CAUTION, "마음을 억누르지 말고 솔직하게 표현해보세요"),
            Suit.SWORD, new CardEssence("생각의 소용돌이", CardLeaning.CAUTION, "머릿속 생각을 잠시 내려놓고 쉬어가 보세요"),
            Suit.PENTACLE, new CardEssence("불안정한 기반", CardLeaning.CAUTION, "조급하게 욕심내기보다 기초부터 다시 다져보세요"));

    public static CardEssence resolve(CardEntity card, boolean reversed) {
        CardEssence essence = card.getArcanaType() == ArcanaType.MAJOR
                ? (reversed ? MAJOR_REVERSED : MAJOR_UPRIGHT).get(card.getNameKr())
                : (reversed ? MINOR_REVERSED : MINOR_UPRIGHT).get(card.getSuit());
        return essence != null ? essence : FALLBACK;
    }
}
