package dev.swote.interv.exception;

import dev.swote.interv.util.MessageConverter;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {
    private final String message;
    private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    public BaseException() {
        this.message = MessageConverter.getMessage("error.common.unknownError");
    }

    public BaseException(String errorCode) {
        this.message = MessageConverter.getMessage(errorCode);
    }

    public BaseException(String errorCode, HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        this.message = MessageConverter.getMessage(errorCode);
    }

    // 추가: 3개 파라미터 생성자 (Custom message 지원)
    public BaseException(String errorCode, HttpStatus httpStatus, String customMessage) {
        this.httpStatus = httpStatus;
        // customMessage가 있으면 사용, 없으면 기본 메시지 사용
        this.message = (customMessage != null && !customMessage.isEmpty())
                ? customMessage
                : MessageConverter.getMessage(errorCode);
    }

    // 추가: 4개 파라미터 생성자 (Throwable cause 지원)
    public BaseException(String errorCode, HttpStatus httpStatus, String customMessage, Throwable cause) {
        super(cause); // cause를 부모 RuntimeException에 전달
        this.httpStatus = httpStatus;
        this.message = (customMessage != null && !customMessage.isEmpty())
                ? customMessage
                : MessageConverter.getMessage(errorCode);
    }

    // 추가: cause만 있는 생성자
    public BaseException(String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
        this.message = MessageConverter.getMessage(errorCode);
    }
}