package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class InterviewStateException extends BaseException {

    public InterviewStateException() {
        super("error.interview.invalid.state", HttpStatus.BAD_REQUEST);
    }

    public InterviewStateException(String message) {
        super("error.interview.invalid.state", HttpStatus.BAD_REQUEST, message);
    }

    public InterviewStateException(String errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}