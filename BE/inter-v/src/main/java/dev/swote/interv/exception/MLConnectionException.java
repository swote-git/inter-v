package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * ML 서버 연결 실패 예외
 */
public class MLConnectionException extends LLMServiceException {

    public MLConnectionException() {
        super("error.llm.connection.failed", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public MLConnectionException(String errorCode) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
