package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BaseException {

    public DuplicateEmailException(String email) {
        super("error.user.duplicateEmail", HttpStatus.CONFLICT);
    }
}