package com.louitarot.auth.dto;

import com.louitarot.auth.entity.UserEntity;

/** [[API 명세]]에서 user 정보를 요약해서 내려줄 때 공통으로 쓰는 형태 (id, nickname, profileImageUrl). */
public record UserSummaryResponse(Long id, String nickname, String profileImageUrl) {

    public static UserSummaryResponse from(UserEntity user) {
        return new UserSummaryResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}
