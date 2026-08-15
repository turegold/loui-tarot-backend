package com.louitarot.chemi.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** chemis(host-guest 관계) 접근용 Spring Data 리포지토리. */
public interface ChemiJpaRepository extends JpaRepository<ChemiEntity, Long> {

    /**
     * 방장 케미 순위 리스트 ([[API 명세]]의 GET /chemi-draws/{hostSlug}/ranking, 2차 기능).
     * idx_host_score(host_draw_id, score DESC) 인덱스를 그대로 타도록 정렬 기준을 맞췄다.
     */
    Page<ChemiEntity> findByHostDrawIdOrderByScoreDesc(Long hostDrawId, Pageable pageable);

    /** 같은 게스트가 같은 방장에게 중복으로 케미를 만들지 않았는지 확인할 때 (uq_host_guest와 대응). */
    Optional<ChemiEntity> findByHostDrawIdAndGuestDrawId(Long hostDrawId, Long guestDrawId);
}
