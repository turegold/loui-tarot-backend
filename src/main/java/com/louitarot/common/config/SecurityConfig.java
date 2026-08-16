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
