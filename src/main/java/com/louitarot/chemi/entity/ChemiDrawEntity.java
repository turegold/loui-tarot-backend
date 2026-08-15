package com.louitarot.chemi.entity;

import com.louitarot.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 케미 뽑기 한 건의 기록 ([[DB 스키마]]의 chemi_draws).
 * 방장이 뽑은 것도, 게스트가 뽑은 것도 전부 이 테이블에 들어간다 — 역할은 chemis 테이블의
 * host/guest 위치로 표현되지, 이 행 자체에 "나는 방장"이라는 플래그가 있는 게 아니다.
 *
 * userId가 nullable인 이유: 방장은 카카오 로그인이 필수라 값이 있지만, 공유 링크로 들어온
 * 게스트는 비로그인이라 null이다. 게스트가 자기 링크를 다시 공유해서 만들어지는 체인상의
 * draw들도 계속 null ([[06 개인정보 및 인증 정책]]).
 */
@Entity
@Table(
        name = "chemi_draws",
        uniqueConstraints = @UniqueConstraint(name = "uq_chemi_draws_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_chemi_draws_user", columnList = "user_id"),
                @Index(name = "idx_chemi_draws_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChemiDrawEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 공유 URL(/chemi/{slug})에 노출되는 식별자. PK를 그대로 쓰지 않는 이유는
     * 링크를 짧게 유지하면서 순번으로 남의 결과를 추측하는 것도 막기 위함.
     */
    @Column(name = "slug", nullable = false, length = 10)
    private String slug;

    /** 로그인한 방장일 때만 채워짐. auth 기능의 users를 객체가 아니라 ID로만 참조한다(기능 간 결합 회피). */
    @Column(name = "user_id")
    private Long userId;

    /** 게스트는 입력값, 방장은 뽑은 시점의 계정 닉네임 스냅샷. 계정 닉네임이 나중에 바뀌어도 이 값은 그대로다. */
    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    /** card 기능의 cards를 ID로만 참조. */
    @Column(name = "card_id", nullable = false)
    private Short cardId;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    /** Rate Limiting / 어뷰징 방지용 SHA-256 해시. 원본 IP는 저장하지 않는다(개인정보 최소 수집). */
    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    public ChemiDrawEntity(String slug, Long userId, String nickname, Short cardId, boolean reversed, String ipHash) {
        this.slug = slug;
        this.userId = userId;
        this.nickname = nickname;
        this.cardId = cardId;
        this.reversed = reversed;
        this.ipHash = ipHash;
    }

    /** 로그인한 방장이 시작한 뽑기인지 (게스트/체인 draw는 false). */
    public boolean isHostedByLoggedInUser() {
        return userId != null;
    }
}
