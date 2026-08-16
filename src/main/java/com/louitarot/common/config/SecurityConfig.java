package com.louitarot.common.config;

import com.louitarot.common.security.JwtAuthenticationEntryPoint;
import com.louitarot.common.security.JwtAuthenticationFilter;
import com.louitarot.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 인증. 카카오 로그인/토큰 재발급 엔드포인트만 공개하고, 나머지 인증 필요 엔드포인트는
 * JwtAuthenticationFilter가 Authorization 헤더의 액세스 토큰을 검증해 통과 여부를 결정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) throws Exception {
        http
                // 세션/쿠키 기반이 아니라 매 요청 JWT로 인증하는 stateless API라 CSRF 보호가 필요 없다.
                .csrf(csrf -> csrf.disable())
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
}
