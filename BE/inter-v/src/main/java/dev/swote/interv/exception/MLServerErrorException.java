package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * ML 서버 내부 오류 예외
 */
public class MLServerErrorException extends LLMServiceException {

    public MLServerErrorException() {
        super("error.llm.server.error", HttpStatus.BAD_GATEWAY);
    }

    public MLServerErrorException(String errorCode) {
        super(errorCode, HttpStatus.BAD_GATEWAY);
    }
}
