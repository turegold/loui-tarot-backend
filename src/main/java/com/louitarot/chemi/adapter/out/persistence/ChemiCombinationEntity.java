package com.louitarot.chemi.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 카드 조합별 케미 점수/해석 캐시 ([[DB 스키마]]의 chemi_combinations).
 * "누가 뽑았는지"와 무관하게 (카드A, 방향A, 카드B, 방향B)만으로 결정되므로 공유 캐시로 둔다.
 *
 * 중요 — 저장 전 정규화: (A,B)와 (B,A)는 같은 궁합인데 그대로 저장하면 캐시가 두 벌 생긴다.
 * 그래서 애플리케이션에서 card_a_id ≤ card_b_id가 되도록 정렬한 뒤 저장/조회한다
 * (카드 ID가 같으면 is_reversed_a ≤ is_reversed_b 기준). 이 정규화는 아래 normalize()로 강제한다.
 */
@Entity
@Table(
        name = "chemi_combinations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_combo",
                columnNames = {"card_a_id", "is_reversed_a", "card_b_id", "is_reversed_b"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChemiCombinationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_a_id", nullable = false)
    private Short cardAId;

    @Column(name = "is_reversed_a", nullable = false)
    private boolean reversedA;

    @Column(name = "card_b_id", nullable = false)
    private Short cardBId;

    @Column(name = "is_reversed_b", nullable = false)
    private boolean reversedB;

    /** 0~100. [[케미 점수 산출 로직]]의 규칙 기반 계산 결과 (AI가 정하는 값이 아님). */
    @Column(name = "score", nullable = false)
    private short score;

    @Column(name = "interpretation_text", nullable = false, columnDefinition = "TEXT")
    private String interpretationText;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    private ChemiCombinationEntity(Short cardAId, boolean reversedA, Short cardBId, boolean reversedB,
                                   short score, String interpretationText) {
        this.cardAId = cardAId;
        this.reversedA = reversedA;
        this.cardBId = cardBId;
        this.reversedB = reversedB;
        this.score = score;
        this.interpretationText = interpretationText;
    }

    /**
     * 두 장을 (a ≤ b) 순서로 정렬해서 생성한다. 생성자를 private으로 막고 이 메서드만 열어둔 이유는,
     * 정규화를 깜빡한 채로 (B,A) 순서 그대로 저장돼서 캐시가 중복 생성되는 걸 막기 위함.
     */
    public static ChemiCombinationEntity normalize(Short cardId1, boolean reversed1,
                                                   Short cardId2, boolean reversed2,
                                                   short score, String interpretationText) {
        boolean inOrder = cardId1 < cardId2
                || (cardId1.equals(cardId2) && !reversed1); // 같은 카드면 정방향(false)을 앞에 둔다
        return inOrder
                ? new ChemiCombinationEntity(cardId1, reversed1, cardId2, reversed2, score, interpretationText)
                : new ChemiCombinationEntity(cardId2, reversed2, cardId1, reversed1, score, interpretationText);
    }
}
