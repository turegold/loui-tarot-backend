package com.louitarot.auth.adapter.out.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** POST https://kauth.kakao.com/oauth/token 응답 중 실제로 쓰는 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoTokenResponse(
        @JsonProperty("access_token") String accessToken
) {
}
