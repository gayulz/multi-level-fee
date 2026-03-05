package com.example.settlement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [NEW] 전역 예외 처리기.
 *
 * REST API 요청 처리 중 발생하는 도메인 및 비즈니스 예외를 공통된 JSON 응답(ErrorResponse) 형태로 변환합니다.
 * RabbitMQ 처리에 대한 재시도(Retry)는 이 컨트롤러 어드바이스를 타지 않고,
 * 인프라(Phase 6) 레벨에서 담당합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 공통 에러 응답 객체 (Java Record)
     */
    public record ErrorResponse(String code, String message) {
    }

    /**
     * [NEW] 정산 노드를 찾지 못한 경우 (404)
     */
    @ExceptionHandler(NodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNodeNotFound(NodeNotFoundException ex) {
        log.warn("Node not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NODE_NOT_FOUND", ex.getMessage()));
    }

    /**
     * [NEW] 정산 계산 중 오류가 발생한 경우 (400)
     */
    @ExceptionHandler(SettlementCalculationException.class)
    public ResponseEntity<ErrorResponse> handleSettlementCalculation(SettlementCalculationException ex) {
        log.warn("Calculation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CALCULATION_ERROR", ex.getMessage()));
    }

    /**
     * [NEW] 잘못된 파라미터가 전달된 경우 (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ARGUMENT", ex.getMessage()));
    }

    /**
     * [NEW] 기타 모든 서버 오류 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
