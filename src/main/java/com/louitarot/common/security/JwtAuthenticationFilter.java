package com.louitarot.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 Bearer 액세스 토큰을 검증해 SecurityContext에 인증 정보를 채운다.
 *
 * 토큰이 아예 없으면 그냥 통과시킨다 — 이 요청이 인증을 요구하는 엔드포인트인지는
 * SecurityConfig의 authorizeHttpRequests가 판단한다. 토큰이 있는데 무효(위조/만료/타입 불일치)면
 * request attribute만 남겨두고 계속 진행한다 — 실제 401 응답 작성은 JwtAuthenticationEntryPoint가
 * (토큰 없음=AUTH_UNAUTHORIZED vs 토큰 무효=AUTH_INVALID_TOKEN을 구분해서) 담당한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_TOKEN_ATTRIBUTE = "jwt.invalid";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtTokenProvider.parseAccessToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, true);
            }
        }
        filterChain.doFilter(request, response);
    }
}
