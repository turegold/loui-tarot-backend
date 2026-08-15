package com.louitarot.chemi.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** chemi_draws 테이블 접근용 Spring Data 리포지토리. */
public interface ChemiDrawJpaRepository extends JpaRepository<ChemiDrawEntity, Long> {

    /** 공유 링크(/chemi/{slug})로 들어온 요청이 원본 뽑기를 찾을 때. */
    Optional<ChemiDrawEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
