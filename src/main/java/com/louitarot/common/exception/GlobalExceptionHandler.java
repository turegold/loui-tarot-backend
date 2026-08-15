package com.louitarot.common.exception;

import com.louitarot.common.response.ApiError;
import com.louitarot.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 백엔드 설계 원칙 5번 — 컨트롤러마다 try-catch를 두지 않고 여기 한 곳에서
 * API 명세.md의 공통 응답 포맷/에러 코드를 강제한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 도메인에서 의도적으로 던진 예외 — 버그가 아니라 예상된 흐름이므로 WARN. */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] {}", errorCode.name(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(ApiError.of(errorCode.name(), e.getMessage())));
    }

    /** @Valid 붙은 @RequestBody 검증 실패. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
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

    /** @Validated 붙은 @RequestParam/@PathVariable 검증 실패. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("[{}] {}", ErrorCode.COMMON_INVALID_REQUEST.name(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ApiError.of(ErrorCode.COMMON_INVALID_REQUEST.name(), e.getMessage())));
    }

    /** 그 외 예상 못한 예외 — 버그일 가능성이 높으므로 ERROR + 스택트레이스. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ApiError.of(
                        ErrorCode.COMMON_INTERNAL_ERROR.name(),
                        ErrorCode.COMMON_INTERNAL_ERROR.getDefaultMessage())));
    }
}
