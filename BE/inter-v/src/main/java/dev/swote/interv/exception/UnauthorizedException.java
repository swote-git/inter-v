package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException() {
        super("error.auth.unauthorized", HttpStatus.UNAUTHORIZED);
    }
}