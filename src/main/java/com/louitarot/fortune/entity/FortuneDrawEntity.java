package com.louitarot.fortune.entity;

import com.louitarot.common.domain.Topic;
import com.louitarot.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 개인 카드 뽑기 한 건 ([[DB 스키마]]의 fortune_draws). 카카오 로그인 필수라 userId는 NOT NULL.
 * 3장 스프레드이므로 항상 자식 카드 3장을 함께 가진다.
 */
@Entity
@Table(
        name = "fortune_draws",
        uniqueConstraints = @UniqueConstraint(name = "uq_fortune_draws_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_fortune_draws_user", columnList = "user_id"),
                @Index(name = "idx_fortune_draws_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FortuneDrawEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** 재방문/공유 URL(/fortune/{slug})용. */
    @Column(name = "slug", nullable = false, length = 10)
    private String slug;

    /** auth 기능의 users를 ID로만 참조 (기능 간 결합 회피). 로그인 필수라 null이 될 수 없다. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 20)
    private Topic topic;

    /**
     * 뽑은 시점의 스프레드 테마 키(예: "time", "emotion"). 자리 라벨 텍스트(과거/현재/미래 등)는
     * DB에 저장하지 않고 이 키 + positionIndex로 애플리케이션이 계산한다 —
     * 테마를 나중에 추가/변경해도 이미 만들어진 결과의 라벨은 그대로 유지되어야 하기 때문
     * ([[02 주요 기능 정의]]의 "스프레드 테마"). 애플리케이션 상수 카탈로그를 가리키는 키라 FK는 아니다.
     */
    @Column(name = "spread_theme_key", nullable = false, length = 20)
    private String spreadThemeKey;

    /**
     * 3장을 종합한 전체 흐름 해석. 3장 조합의 경우의 수(약 370만)가 너무 많아 공유 캐시를 만들 수 없어서
     * chemi_combinations처럼 캐시 테이블로 빼지 않고 이 행에 직접 저장한다 ([[AI 해석 캐싱 전략]]).
     */
    @Column(name = "overall_interpretation_text", nullable = false, columnDefinition = "TEXT")
    private String overallInterpretationText;

    /**
     * 자식 카드 3장. 여기만 @OneToMany 객체 참조를 쓰는 이유는 진짜 부모-자식 소유 관계라서다 —
     * 뽑기 없이 자식 카드만 존재할 이유가 없고, 항상 함께 저장/조회된다.
     * cascade + orphanRemoval로 뽑기를 저장하면 카드 3장도 같이 저장되게 했다.
     */
    @OneToMany(mappedBy = "fortuneDraw", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionIndex ASC") // 0,1,2 순서 = 테마 자리 순서가 항상 보장되도록
    private List<FortuneDrawCardEntity> cards = new ArrayList<>();

    public FortuneDrawEntity(String slug, Long userId, Topic topic,
                             String spreadThemeKey, String overallInterpretationText) {
        this.slug = slug;
        this.userId = userId;
        this.topic = topic;
        this.spreadThemeKey = spreadThemeKey;
        this.overallInterpretationText = overallInterpretationText;
    }

    /**
     * 자식 카드를 추가하면서 양쪽 참조를 함께 맞춰준다.
     * 이런 메서드 없이 draw.getCards().add(card)만 하면 자식 쪽 fortuneDraw가 null이라
     * FK 컬럼이 안 채워진 채로 저장되는 흔한 버그가 생긴다.
     */
    public void addCard(FortuneDrawCardEntity card) {
        cards.add(card);
        card.assignTo(this);
    }

    /** 외부에서 리스트를 직접 조작해 위 규칙을 우회하지 못하도록 읽기 전용으로 노출. */
    public List<FortuneDrawCardEntity> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
