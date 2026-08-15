package com.louitarot.auth.repository;

import com.louitarot.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** users 테이블 접근용 Spring Data 리포지토리. */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 카카오 로그인 콜백에서 "기존 회원인지 신규 가입인지" 판단할 때 쓴다
     * ([[API 명세]]의 POST /auth/kakao/callback이 내려주는 isNewUser).
     */
    Optional<UserEntity> findByKakaoId(Long kakaoId);
}
