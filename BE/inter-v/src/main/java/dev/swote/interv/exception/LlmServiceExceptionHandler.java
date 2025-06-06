package dev.swote.interv.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class LlmServiceExceptionHandler {

    @ExceptionHandler(LLMServiceException.class)
    public ResponseEntity<Map<String, Object>> handleLlmServiceException(LLMServiceException e) {
        log.error("LLM 서비스 오류: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.put("error", "LLM Service Error");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("suggestion", "잠시 후 다시 시도해주세요. 문제가 지속되면 관리자에게 문의하세요.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 오류: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "서버에서 오류가 발생했습니다.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}