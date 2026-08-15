package com.louitarot.auth.adapter.out.persistence;

import com.louitarot.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카카오 로그인 계정 ([[DB 스키마]]의 users). MVP 인증 수단은 카카오 하나뿐.
 *
 * 개인정보 최소 수집 원칙([[06 개인정보 및 인증 정책]])에 따라 카카오에서 받아오는 것은
 * 회원번호/닉네임/프로필 이미지까지만이다 — 이메일·전화번호·생년월일은 저장하지 않는다.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_kakao_id", columnNames = "kakao_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** 카카오가 발급하는 회원번호. 우리 서비스의 로그인 식별자. */
    @Column(name = "kakao_id", nullable = false)
    private Long kakaoId;

    /**
     * 최초 로그인 시 카카오 프로필 닉네임으로 초기화되지만, 이후에는 앱 내에서 자유롭게 수정 가능하고
     * 카카오 쪽 닉네임이 바뀌어도 따라 변하지 않는다 (앱 내 닉네임으로 독립 관리).
     */
    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    public UserEntity(Long kakaoId, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = LocalDateTime.now();
    }

    // ↓ 무분별한 setter 대신 "무엇을 하는 변경인지" 드러나는 메서드만 연다.
    //   엔티티가 어떤 이유로 바뀔 수 있는지가 메서드 목록만 봐도 드러나게 하기 위함.

    /** 마이페이지에서 앱 내 닉네임을 바꿀 때. */
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 카카오 로그인에 성공할 때마다 갱신. */
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
