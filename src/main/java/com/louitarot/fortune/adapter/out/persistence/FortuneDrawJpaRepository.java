package com.louitarot.fortune.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** fortune_draws 테이블 접근용 Spring Data 리포지토리. */
public interface FortuneDrawJpaRepository extends JpaRepository<FortuneDrawEntity, Long> {

    /** 결과 페이지(/fortune/{slug}) 조회용. 공유 링크라 비로그인도 볼 수 있다. */
    Optional<FortuneDrawEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * 마이페이지 "내 기록" 목록 ([[API 명세]]의 GET /users/me/fortunes).
     * 기록이 계속 쌓이는 화면이라 여기서 페이지네이션(ApiMeta)이 실제로 쓰인다.
     */
    Page<FortuneDrawEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
