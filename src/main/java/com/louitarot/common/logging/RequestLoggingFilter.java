package com.louitarot.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 requestId를 MDC에 심어서(그 요청 동안 찍히는 모든 JSON 로그 줄에 자동 포함) 응답이 끝나면
 * method/path/status/durationMs 한 줄을 로그로 남긴다. 헤더나 바디는 찍지 않는다 —
 * 개인정보/토큰 원문을 로그에 남기지 않는다는 원칙(백엔드 설계 원칙 9번)과 부딪히기 때문.
 *
 * OncePerRequestFilter를 상속한 이유: 서블릿 컨테이너 내부에서 요청이 forward/include로
 * 재처리되는 경우에도 이 필터가 요청당 딱 한 번만 실행되도록 스프링이 보장해주는 베이스 클래스라서.
 * @Component를 붙이면 스프링 부트가 이 빈을 자동으로 필터 체인에 등록한다(별도 등록 코드 불필요).
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // MDC = 현재 스레드에 붙는 키-값 저장소. 여기 넣어두면, 이 요청을 처리하는 동안
        // (컨트롤러, 서비스, GlobalExceptionHandler 어디서든) 찍히는 모든 로그 줄에
        // requestId가 자동으로 필드로 딸려 나간다 — 매번 파라미터로 넘길 필요가 없음.
        MDC.put(REQUEST_ID_MDC_KEY, generateRequestId());
        long startedAtMs = System.currentTimeMillis();
        try {
            // 실제 컨트롤러 로직은 이 한 줄 안에서 실행된다. 여기서 예외가 터져도
            // (GlobalExceptionHandler가 잡아서 응답은 만들어주지만) 아래 finally는 반드시 실행된다.
            filterChain.doFilter(request, response);
        } finally {
            // try/finally로 감싼 이유: 요청이 성공하든 실패하든(예외가 나든) 반드시
            //   1) 로그 한 줄은 남겨야 하고 (오히려 실패한 요청이 더 중요한 로그다)
            //   2) MDC는 반드시 정리해야 한다 — 안 지우면, 톰캣이 스레드를 재사용하기 때문에
            //      다음 요청이 이 스레드를 물려받으면서 엉뚱하게 이전 requestId를 로그에 물고 나온다.
            long durationMs = System.currentTimeMillis() - startedAtMs;
            // response.getStatus()는 doFilter가 끝난 "지금" 읽어야 최종 상태코드가 맞다
            // (GlobalExceptionHandler가 500/404 등으로 바꿔놓은 뒤의 값을 읽는 것).
            log.info("method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String generateRequestId() {
        // 전체 UUID(36자)는 로그에서 너무 기니까 앞 8자만 — 같은 순간에 겹칠 확률은 실무적으로 무시 가능
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
