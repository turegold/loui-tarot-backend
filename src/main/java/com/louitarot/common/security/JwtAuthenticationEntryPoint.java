package com.louitarot.common.security;

import com.louitarot.common.exception.ErrorCode;
import com.louitarot.common.response.ApiError;
import com.louitarot.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 인증 실패를 Spring Security 기본(빈 401 응답)이 아니라 API 명세.md의 공통 응답 포맷으로 내려준다.
 *
 * 이 예외는 DispatcherServlet 이전(필터 단계)에서 발생해서 GlobalExceptionHandler가 못 잡는다 —
 * 그래서 여기서 직접 ApiResponse를 만들어 응답에 쓴다. JwtAuthenticationFilter가 남긴 표시로
 * "토큰이 아예 없음(AUTH_UNAUTHORIZED)"과 "토큰은 있는데 무효함(AUTH_INVALID_TOKEN)"을 구분한다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorCode errorCode = request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE) != null
                ? ErrorCode.AUTH_INVALID_TOKEN
                : ErrorCode.AUTH_UNAUTHORIZED;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ApiError.of(errorCode.name(), errorCode.getDefaultMessage()))));
    }
}
