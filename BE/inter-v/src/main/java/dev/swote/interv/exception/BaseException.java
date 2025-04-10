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
}