package com.louitarot.chemi.repository;

import com.louitarot.chemi.entity.ChemiDrawEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** chemi_draws 테이블 접근용 Spring Data 리포지토리. */
public interface ChemiDrawJpaRepository extends JpaRepository<ChemiDrawEntity, Long> {

    /** 공유 링크(/chemi/{slug})로 들어온 요청이 원본 뽑기를 찾을 때. */
    Optional<ChemiDrawEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * 마이페이지 "내 기록"의 케미 뽑기 목록 ([[API 명세]]의 GET /users/me/chemi-draws).
     * 로그인한 방장으로 뽑은 것만 대상 — userId가 있는 행만 이 유저 소유다(게스트 draw는 null).
     */
    Page<ChemiDrawEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
