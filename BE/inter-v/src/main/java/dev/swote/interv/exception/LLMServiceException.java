package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class LLMServiceException extends BaseException {

    public LLMServiceException() {
        super("error.llm.service.failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LLMServiceException(String message) {
        super("error.llm.service.failed", HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public LLMServiceException(String message, Throwable cause) {
        super("error.llm.service.failed", HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public LLMServiceException(String errorCode, String message) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}