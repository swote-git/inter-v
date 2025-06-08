package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * LLM/ML 서비스 관련 기본 예외 클래스
 */
public class LLMServiceException extends BaseException {

    public LLMServiceException() {
        super("error.llm.service.failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LLMServiceException(String errorCode) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LLMServiceException(String errorCode, HttpStatus httpStatus) {
        super(errorCode, httpStatus);
    }
}
