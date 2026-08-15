package com.louitarot.card.repository;

import com.louitarot.card.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * cards 테이블 접근용 Spring Data 리포지토리. Service가 이 인터페이스를 직접 주입받아 쓴다 —
 * DB 접근에는 포트/어댑터를 따로 두지 않는다. JpaRepository 자체가 이미 인터페이스라 그 위에
 * 한 겹 더 감싸는 게 실익이 없기 때문 ([[백엔드 설계 원칙]] 1번, 헥사고날은 AI API·카카오
 * 로그인처럼 원래 인터페이스가 없던 자리에만 적용).
 */
public interface CardJpaRepository extends JpaRepository<CardEntity, Short> {

    /** SEO 카드 상세 페이지(/card/{seoSlug}) 조회용. */
    Optional<CardEntity> findBySeoSlug(String seoSlug);
}
