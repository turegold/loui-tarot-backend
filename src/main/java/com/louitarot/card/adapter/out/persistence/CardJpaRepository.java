package com.louitarot.card.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * cards 테이블 접근용 Spring Data 리포지토리.
 *
 * 이름에 Jpa를 넣은 이유: 나중에 유스케이스가 생기면 도메인이 필요로 하는 형태의
 * 아웃바운드 포트(예: CardRepository 인터페이스)를 따로 두고, 이 클래스가 그 포트를
 * 구현하는 어댑터가 된다 ([[백엔드 설계 원칙]] 1번). 지금은 포트를 쓸 유스케이스가
 * 아직 없어서 만들지 않았다 — 호출자 없는 인터페이스를 미리 만들지 않는다(8번 YAGNI).
 */
public interface CardJpaRepository extends JpaRepository<CardEntity, Short> {

    /** SEO 카드 상세 페이지(/card/{seoSlug}) 조회용. */
    Optional<CardEntity> findBySeoSlug(String seoSlug);
}
