package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * ML API 응답 파싱 실패 예외
 */
public class MLResponseParsingException extends LLMServiceException {

    public MLResponseParsingException() {
        super("error.llm.response.parsing.failed", HttpStatus.BAD_GATEWAY);
    }

    public MLResponseParsingException(String errorCode) {
        super(errorCode, HttpStatus.BAD_GATEWAY);
    }
}