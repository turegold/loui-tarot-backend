package com.louitarot.auth.adapter.out.kakao;

import com.louitarot.auth.application.port.out.KakaoAuthPort;
import com.louitarot.auth.application.port.out.KakaoUserInfo;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * KakaoAuthPort의 실제 구현체 — 카카오 인가코드 → 액세스 토큰 → 사용자 정보까지의 실제 HTTP 호출.
 * AuthService는 이 클래스의 존재를 모르고 KakaoAuthPort 인터페이스만 의존한다.
 */
@Component
public class KakaoAuthAdapter implements KakaoAuthPort {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthAdapter.class);
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoAuthAdapter(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret:}") String clientSecret,
            @Value("${kakao.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public KakaoUserInfo fetchUserInfo(String authorizationCode) {
        try {
            String kakaoAccessToken = exchangeToken(authorizationCode);
            return fetchProfile(kakaoAccessToken);
        } catch (RestClientException e) {
            log.warn("카카오 로그인 실패", e);
            throw new CustomException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED);
        }
    }

    private String exchangeToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        // 카카오 콘솔에서 "Client Secret 사용"이 꺼져 있으면 이 값 자체가 비어 있어 안 보냄
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }

        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new CustomException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED);
        }
        return response.accessToken();
    }

    private KakaoUserInfo fetchProfile(String kakaoAccessToken) {
        KakaoUserResponse response = restClient.get()
                .uri(USER_INFO_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class);

        if (response == null || response.id() == null) {
            throw new CustomException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED);
        }

        KakaoUserResponse.KakaoAccount.Profile profile = response.kakaoAccount() != null
                ? response.kakaoAccount().profile()
                : null;
        KakaoUserResponse.Properties properties = response.properties();

        // kakao_account.profile은 동의항목 설정에 따라 비어 있을 수 있어, 레거시 properties를 폴백으로 쓴다.
        String nickname = profile != null && profile.nickname() != null
                ? profile.nickname()
                : (properties != null ? properties.nickname() : null);
        String profileImageUrl = profile != null && profile.profileImageUrl() != null
                ? profile.profileImageUrl()
                : (properties != null ? properties.profileImage() : null);

        return new KakaoUserInfo(response.id(), nickname, profileImageUrl);
    }
}
