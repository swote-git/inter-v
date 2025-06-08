package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * ML API 요청 형식 오류 예외
 */
public class MLBadRequestException extends LLMServiceException {

    public MLBadRequestException() {
        super("error.llm.request.invalid", HttpStatus.BAD_REQUEST);
    }

    public MLBadRequestException(String errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }
}