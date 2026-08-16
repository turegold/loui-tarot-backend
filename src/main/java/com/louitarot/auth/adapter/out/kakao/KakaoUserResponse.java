package com.louitarot.auth.adapter.out.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET https://kapi.kakao.com/v2/user/me 응답 중 실제로 쓰는 필드만 (닉네임/프로필 사진).
 *
 * 닉네임/프로필 사진은 두 경로로 내려올 수 있다 — 신형 {@code kakao_account.profile}(동의항목
 * 기반)과 구형 {@code properties}(레거시, 대부분 앱에서 기본으로 채워짐). 콘솔의 동의항목 설정에
 * 따라 kakao_account 쪽이 비어 있을 수 있어 properties를 폴백으로 함께 받아둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount,
        @JsonProperty("properties") Properties properties
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(
            @JsonProperty("profile") Profile profile
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Profile(
                @JsonProperty("nickname") String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl
        ) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("profile_image") String profileImage
    ) {
    }
}
