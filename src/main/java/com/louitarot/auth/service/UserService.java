package com.louitarot.auth.service;

import com.louitarot.auth.entity.UserEntity;
import com.louitarot.auth.repository.UserJpaRepository;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 마이페이지 프로필 조회/수정(레이어드). 로그인/토큰 흐름은 AuthService가 담당하고, 여긴 순수 유저 정보만. */
@Service
public class UserService {

    private final UserJpaRepository userJpaRepository;

    public UserService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional(readOnly = true)
    public UserEntity getProfile(Long userId) {
        return findUser(userId);
    }

    /** 카카오 프로필 닉네임과 무관하게 앱 내 닉네임만 독립적으로 바꾼다. */
    @Transactional
    public UserEntity updateNickname(Long userId, String nickname) {
        UserEntity user = findUser(userId);
        user.changeNickname(nickname);
        return user;
    }

    private UserEntity findUser(Long userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
    }
}
