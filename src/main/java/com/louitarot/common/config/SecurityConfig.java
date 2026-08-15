package com.louitarot.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 임시 보안 설정. JWT 인증(카카오 로그인)이 아직 없어서, 지금은 "API 명세상 공개인 엔드포인트만
 * 열어주고 나머지는 스프링 시큐리티 기본 Basic Auth로 막는다"는 최소 상태다.
 * JWT 필터를 붙이는 인증 기능 작업에서 이 클래스를 교체한다 — httpBasic은 그때 제거.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 세션/쿠키 기반이 아니라 매 요청 토큰(추후 JWT)으로 인증할 stateless API라 CSRF 보호가
                // 필요 없다 — 켜져 있으면 Postman에서 GET 이외 요청 보낼 때마다 불필요하게 막힌다.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // [[API 명세]] 기준 인증 불필요 엔드포인트
                        .requestMatchers(HttpMethod.GET, "/api/v1/cards", "/api/v1/cards/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        // 그 외는 전부 인증 필요 — 아직 JWT가 없어서 당장은 자동 생성된 Basic Auth 비밀번호로만 통과됨
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
