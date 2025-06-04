package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BaseException {

    public AuthenticationFailedException() {
        super("error.auth.failed", HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationFailedException(String errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}