package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException() {
        super("error.token.invalid", HttpStatus.UNAUTHORIZED);
    }

    public InvalidTokenException(String errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}