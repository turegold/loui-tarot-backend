package com.louitarot.auth.adapter.out.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** GET https://kapi.kakao.com/v2/user/me 응답 중 실제로 쓰는 필드만 (닉네임/프로필 사진). */
@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
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
}
