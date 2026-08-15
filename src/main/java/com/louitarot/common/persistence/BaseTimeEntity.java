package com.louitarot.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * created_at 컬럼을 가진 엔티티들의 공통 부모.
 *
 * @MappedSuperclass는 "이 클래스는 테이블이 아니지만, 상속받는 엔티티에 필드를 물려준다"는 뜻.
 * 즉 base_time_entity 테이블이 생기는 게 아니라, cards/users/chemi_draws 등 각 테이블에
 * created_at 컬럼이 하나씩 생긴다.
 *
 * 해석 캐시 테이블들은 created_at이 아니라 generated_at을 쓰고(의미가 "생성 시각"이 아니라
 * "AI가 해석을 만든 시각"), fortune_draw_cards는 아예 타임스탬프가 없어서 이 클래스를 안 쓴다.
 */
@MappedSuperclass
public abstract class BaseTimeEntity {

    /** insert 시점에 Hibernate가 자동으로 채운다 (@CreationTimestamp). 이후 수정되지 않음. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
