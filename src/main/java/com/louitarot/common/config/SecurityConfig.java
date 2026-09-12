package com.louitarot.common.config;

import com.louitarot.common.security.JwtAuthenticationEntryPoint;
import com.louitarot.common.security.JwtAuthenticationFilter;
import com.louitarot.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * JWT 기반 인증. 카카오 로그인/토큰 재발급 엔드포인트만 공개하고, 나머지 인증 필요 엔드포인트는
 * JwtAuthenticationFilter가 Authorization 헤더의 액세스 토큰을 검증해 통과 여부를 결정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String frontendUrl;
    /** 같은 와이파이의 휴대폰 등으로 모바일 테스트할 때만 채우는 로컬 전용 값 — 비어 있으면 무시. */
    private final String frontendLanUrl;

    public SecurityConfig(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.frontend-lan-url:}") String frontendLanUrl
    ) {
        this.frontendUrl = frontendUrl;
        this.frontendLanUrl = frontendLanUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) throws Exception {
        http
                // 세션/쿠키 기반이 아니라 매 요청 JWT로 인증하는 stateless API라 CSRF 보호가 필요 없다.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // [[API 명세]] 기준 인증 불필요 엔드포인트
                        .requestMatchers(HttpMethod.GET, "/api/v1/cards", "/api/v1/cards/**").permitAll()
                        .requestMatchers("/api/v1/auth/kakao/callback", "/api/v1/auth/refresh").permitAll()
                        // 케미 뽑기: 게스트 참여(POST .../guests)와 결과 조회(GET)는 비로그인 공개,
                        // 방장 최초 뽑기(POST /chemi-draws)는 로그인 필수라 anyRequest().authenticated()에 그대로 걸림
                        .requestMatchers(HttpMethod.POST, "/api/v1/chemi-draws/*/guests").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chemi-draws/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chemi-draws/*/ranking").permitAll()
                        // 개인 카드 뽑기: 결과 공유 링크 조회(GET)는 공개, 뽑기 생성(POST)은 로그인 필수
                        .requestMatchers(HttpMethod.GET, "/api/v1/fortunes/*").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        // 그 외는 전부 인증 필요 (예: /api/v1/auth/logout, 앞으로 만들 fortunes/chemi-draws 방장 엔드포인트)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 프론트(app.frontend-url — chemi/fortune의 shareUrl 조합에도 쓰는 같은 값)에서 브라우저로
     * 직접 fetch할 수 있게 허용한다. 이게 없으면 Authorization 헤더가 실려 있어도 브라우저가
     * preflight(OPTIONS) 응답에서 Access-Control-Allow-Origin을 못 찾아 요청 자체를 막아버린다
     * (서버 로그엔 OPTIONS 200이 찍히지만, 그 응답에 CORS 헤더가 없어서 브라우저가 실패시키는 것).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = frontendLanUrl.isBlank()
                ? List.of(frontendUrl)
                : List.of(frontendUrl, frontendLanUrl);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
