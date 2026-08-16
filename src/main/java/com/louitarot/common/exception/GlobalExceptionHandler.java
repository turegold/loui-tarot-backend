package com.louitarot.common.exception;

import com.louitarot.common.response.ApiError;
import com.louitarot.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 백엔드 설계 원칙 5번 — 컨트롤러마다 try-catch를 두지 않고 여기 한 곳에서
 * API 명세.md의 공통 응답 포맷/에러 코드를 강제한다.
 *
 * 동작 원리: @RestControllerAdvice가 붙으면 스프링이 "모든 컨트롤러에서 예외가 발생하면
 * 여길 거쳐가라"고 등록해준다. 컨트롤러(혹은 그 안에서 호출한 서비스)에서 예외가 터지면,
 * 스프링이 그 예외의 타입을 보고 아래 @ExceptionHandler들 중 가장 정확히 일치하는 메서드를
 * 자동으로 찾아 실행한다 — 선언 순서는 상관없고, 타입 매칭이 기준이다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * ① 도메인에서 의도적으로 던진 예외 (throw new CustomException(...)).
     * 버그가 아니라 "존재하지 않는 카드예요" 같은 예상된 흐름이므로 WARN.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] {}", errorCode.name(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())                                  // ErrorCode에 박아둔 상태코드 그대로
                .body(ApiResponse.fail(ApiError.of(errorCode.name(), e.getMessage())));
    }

    /**
     * ② @RequestBody DTO에 @Valid를 붙였을 때, 그 안의 @NotBlank/@Size 같은 제약이
     * 깨지면 스프링이 자동으로 이 예외를 던진다 (우리가 직접 던지는 게 아님).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 실패한 필드들을 하나씩 꺼내서 "어떤 필드가 왜 틀렸는지" 리스트로 만든다 → ApiError.details
        List<ApiError.FieldDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        log.warn("[{}] {}", ErrorCode.COMMON_INVALID_REQUEST.name(), details);
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ApiError.of(
                        ErrorCode.COMMON_INVALID_REQUEST.name(),
                        ErrorCode.COMMON_INVALID_REQUEST.getDefaultMessage(),
                        details)));
    }

    /**
     * ③ @RequestParam/@PathVariable을 (@Valid가 아니라) 클래스 레벨 @Validated로
     * 검증할 때 실패하면 이 예외가 던져진다 — ②랑 트리거되는 상황이 다를 뿐 처리는 동일.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("[{}] {}", ErrorCode.COMMON_INVALID_REQUEST.name(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ApiError.of(ErrorCode.COMMON_INVALID_REQUEST.name(), e.getMessage())));
    }

    /**
     * ④ 요청 바디 JSON 자체는 문법적으로 읽었지만, 값이 기대하는 타입과 안 맞을 때
     * (예: {@code topic}에 enum에 없는 문자열, 숫자 자리에 문자열 등). @Valid보다 앞서
     * JSON→객체 역직렬화 단계에서 터지는 예외라 MethodArgumentNotValidException으로는 안 잡힌다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[{}] {}", ErrorCode.COMMON_INVALID_REQUEST.name(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ApiError.of(
                        ErrorCode.COMMON_INVALID_REQUEST.name(),
                        ErrorCode.COMMON_INVALID_REQUEST.getDefaultMessage())));
    }

    /**
     * ⑤ 위 넷 중 아무것도 안 걸리는 나머지 전부(진짜 버그, DB 커넥션 끊김 등).
     * 여기까지 흘러왔다는 것 자체가 "예상 못 했다"는 뜻이라 ERROR + 스택트레이스로 남긴다.
     * 이 핸들러가 없으면 예상 못 한 예외는 우리 응답 포맷이 아니라 스프링 기본 에러 페이지로
     * 나가버린다 — 그래서 Exception.class(가장 넓은 타입)까지 반드시 잡아둔다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("Unhandled exception", e);   // e를 두 번째 인자로 넘기면 스택트레이스까지 로그에 포함됨
        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ApiError.of(
                        ErrorCode.COMMON_INTERNAL_ERROR.name(),
                        ErrorCode.COMMON_INTERNAL_ERROR.getDefaultMessage())));
    }
}
