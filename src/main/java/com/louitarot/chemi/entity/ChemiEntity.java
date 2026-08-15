package com.louitarot.chemi.entity;

import com.louitarot.common.entity.BaseTimeEntity;
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
 * 실제로 성립한 host-guest 케미 관계 ([[DB 스키마]]의 chemis).
 * 한 chemi_draw가 여러 관계에서 host로도 guest로도 등장할 수 있는 N:N 구조를 이 테이블이 풀어낸다.
 *
 * 여기서는 @ManyToOne 객체 참조를 쓴다 — chemi 기능 내부(chemis ↔ chemi_draws)라
 * 기능 간 결합이 생기지 않고, 방장 순위 조회에서 게스트 정보(닉네임/카드)를 함께 읽어야 해서
 * join으로 한 번에 가져오는 게 유리하기 때문. (반대로 users/cards처럼 다른 기능 것은 ID로만 참조)
 */
@Entity
@Table(
        name = "chemis",
        uniqueConstraints = @UniqueConstraint(name = "uq_host_guest", columnNames = {"host_draw_id", "guest_draw_id"}),
        indexes = {
                // 방장 순위 리스트(WHERE host_draw_id = ? ORDER BY score DESC)가 최다 호출 read-path
                @Index(name = "idx_host_score", columnList = "host_draw_id, score DESC"),
                @Index(name = "idx_guest", columnList = "guest_draw_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChemiEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * fetch = LAZY를 명시하는 이유: @ManyToOne의 기본값은 EAGER라서, chemis 한 행만 읽어도
     * 연결된 draw들을 항상 같이 조회해버린다(순위 목록처럼 여러 건 읽을 때 N+1의 원인).
     * 필요할 때만 fetch join으로 명시적으로 가져오는 편이 낫다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_draw_id", nullable = false)
    private ChemiDrawEntity hostDraw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_draw_id", nullable = false)
    private ChemiDrawEntity guestDraw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemi_combination_id", nullable = false)
    private ChemiCombinationEntity combination;

    /**
     * chemi_combinations.score를 의도적으로 비정규화 복제한 값 ([[DB 스키마]] 설계 원칙).
     * 순위 정렬을 join 없이 이 테이블만으로 끝내기 위함이고, 점수는 규칙 기반이라 결정론적이어서
     * 원본이 바뀔 일이 없다 → 캐시 무효화를 걱정할 필요가 없다.
     */
    @Column(name = "score", nullable = false)
    private short score;

    public ChemiEntity(ChemiDrawEntity hostDraw, ChemiDrawEntity guestDraw,
                       ChemiCombinationEntity combination, short score) {
        this.hostDraw = hostDraw;
        this.guestDraw = guestDraw;
        this.combination = combination;
        this.score = score;
    }
}
