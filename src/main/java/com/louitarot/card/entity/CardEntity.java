package com.louitarot.card.entity;

import com.louitarot.card.domain.ArcanaType;
import com.louitarot.card.domain.Element;
import com.louitarot.card.domain.Suit;
import com.louitarot.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타로 78장 마스터 데이터 ([[DB 스키마]]의 cards).
 *
 * 앱에서 생성/수정하지 않는 시드 데이터라 id에 @GeneratedValue가 없다 — 1~78을 직접 지정해서 넣는다.
 * 행이 78개로 고정이고 절대 안 바뀌므로, 나중에 조회가 잦아지면 통째로 캐싱하기 좋은 대상이다.
 */
@Entity
@Table(name = "cards", uniqueConstraints = @UniqueConstraint(name = "uq_cards_seo_slug", columnNames = "seo_slug"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 리플렉션으로 객체를 만들 때만 쓰는 기본 생성자
public class CardEntity extends BaseTimeEntity {

    /** 1~78. TINYINT UNSIGNED라 Java에서는 Short로 받는다. */
    @Id
    @Column(name = "id", nullable = false)
    private Short id;

    @Column(name = "name_kr", nullable = false, length = 50)
    private String nameKr;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    /**
     * EnumType.STRING 필수 — 기본값(ORDINAL)은 enum 선언 순서를 숫자로 저장해서,
     * 나중에 enum 상수 순서만 바꿔도 기존 데이터의 의미가 통째로 어긋난다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "arcana_type", nullable = false, length = 10)
    private ArcanaType arcanaType;

    /** 메이저 아르카나는 수트가 없어서 null. */
    @Enumerated(EnumType.STRING)
    @Column(name = "suit", length = 10)
    private Suit suit;

    /** 메이저 아르카나는 원소를 쓰지 않아서 null (Element 주석 참고). */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", length = 10)
    private Element element;

    /** 메이저는 0~21, 마이너는 1~14(에이스~킹). */
    @Column(name = "number")
    private Short number;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    /** SEO 카드 상세 페이지 URL(/card/{seoSlug})에 쓰이는 식별자. */
    @Column(name = "seo_slug", nullable = false, length = 60)
    private String seoSlug;

    public CardEntity(Short id, String nameKr, String nameEn, ArcanaType arcanaType,
                      Suit suit, Element element, Short number, String imageUrl, String seoSlug) {
        this.id = id;
        this.nameKr = nameKr;
        this.nameEn = nameEn;
        this.arcanaType = arcanaType;
        this.suit = suit;
        this.element = element;
        this.number = number;
        this.imageUrl = imageUrl;
        this.seoSlug = seoSlug;
    }
}
