package com.louitarot.card.adapter.out.persistence;

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
 * 케미 뽑기용 범용(주제 없음) 카드 해석 캐시 ([[AI 해석 캐싱 전략]]의 card_interpretations).
 * 78장 × 정/역방향 = 최대 156행으로 유한하므로 사전 프리컴퓨팅 대상.
 *
 * cards를 @ManyToOne 객체가 아니라 cardId(Long/Short)로만 참조하는 이유:
 * 이 테이블은 카드에 "속한" 자식이 아니라, 카드를 키로 삼는 독립된 캐시 행이다.
 * (부모-자식 소유 관계인 fortune_draws ↔ fortune_draw_cards만 객체 참조를 쓴다)
 */
@Entity
@Table(
        name = "card_interpretations",
        uniqueConstraints = @UniqueConstraint(name = "uq_card_dir", columnNames = {"card_id", "is_reversed"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardInterpretationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Short cardId;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Column(name = "interpretation_text", nullable = false, columnDefinition = "TEXT")
    private String interpretationText;

    /** "행이 만들어진 시각"이 아니라 "AI가 이 해석을 생성한 시각"이라 created_at과 이름을 구분한다. */
    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    public CardInterpretationEntity(Short cardId, boolean reversed, String interpretationText) {
        this.cardId = cardId;
        this.reversed = reversed;
        this.interpretationText = interpretationText;
    }
}
