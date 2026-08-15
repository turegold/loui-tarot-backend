package com.louitarot.card.adapter.out.persistence;

import com.louitarot.common.domain.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 개인 카드 뽑기용 주제별 카드 해석 캐시 ([[AI 해석 캐싱 전략]]의 card_topic_interpretations).
 * 156 × 4주제 = 최대 624행. 역시 프리컴퓨팅 대상.
 *
 * 중요: 스프레드 자리(과거/현재/미래 등)와 무관하게 (카드, 방향, 주제)만으로 캐싱한다 —
 * 자리 라벨은 매일 바뀌지만([[02 주요 기능 정의]]의 스프레드 테마) 카드 해석 자체는 자리와 무관하므로,
 * 자리를 키에 넣으면 캐시가 쓸데없이 잘게 쪼개진다.
 */
@Entity
@Table(
        name = "card_topic_interpretations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_card_dir_topic",
                columnNames = {"card_id", "is_reversed", "topic"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTopicInterpretationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Short cardId;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 20)
    private Topic topic;

    @Column(name = "interpretation_text", nullable = false, columnDefinition = "TEXT")
    private String interpretationText;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    public CardTopicInterpretationEntity(Short cardId, boolean reversed, Topic topic, String interpretationText) {
        this.cardId = cardId;
        this.reversed = reversed;
        this.topic = topic;
        this.interpretationText = interpretationText;
    }
}
