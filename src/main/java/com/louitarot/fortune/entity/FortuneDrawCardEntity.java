package com.louitarot.fortune.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개인 카드 뽑기의 자리별 카드 한 장 ([[DB 스키마]]의 fortune_draw_cards). 뽑기 1건당 3행.
 *
 * 자리 "이름"(과거/현재/미래 등)을 저장하지 않고 순서(positionIndex 0~2)만 저장하는 게 핵심이다 —
 * 라벨은 뽑기의 spreadThemeKey와 이 인덱스를 조합해 애플리케이션이 계산하므로,
 * 스프레드 테마를 몇 종류로 늘리든 스키마를 바꿀 필요가 없다.
 */
@Entity
@Table(
        name = "fortune_draw_cards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_fortune_draw_position",
                columnNames = {"fortune_draw_id", "position_index"}), // 같은 자리에 두 장이 들어가는 걸 DB가 막아준다
        indexes = @Index(name = "idx_fortune_draw_cards_draw", columnList = "fortune_draw_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FortuneDrawCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** 부모 뽑기. fortune 기능 내부의 부모-자식 관계라 객체 참조를 쓴다(LAZY는 필수). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fortune_draw_id", nullable = false)
    private FortuneDrawEntity fortuneDraw;

    /** 0, 1, 2 — 뽑은 순서이자 스프레드 테마의 자리 순서. */
    @Column(name = "position_index", nullable = false)
    private short positionIndex;

    /** card 기능의 cards를 ID로만 참조. */
    @Column(name = "card_id", nullable = false)
    private Short cardId;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    public FortuneDrawCardEntity(short positionIndex, Short cardId, boolean reversed) {
        this.positionIndex = positionIndex;
        this.cardId = cardId;
        this.reversed = reversed;
    }

    /** 부모 쪽 addCard()에서만 호출된다 — 양방향 참조를 한 곳에서만 맞추기 위해 패키지 전용으로 제한. */
    void assignTo(FortuneDrawEntity fortuneDraw) {
        this.fortuneDraw = fortuneDraw;
    }
}
