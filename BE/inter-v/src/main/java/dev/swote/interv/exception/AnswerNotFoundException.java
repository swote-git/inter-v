package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class AnswerNotFoundException extends BaseException {

    public AnswerNotFoundException() {
        super("error.answer.not.found", HttpStatus.NOT_FOUND);
    }

    public AnswerNotFoundException(Integer answerId) {
        super("error.answer.not.found", HttpStatus.NOT_FOUND, "Answer not found with id: " + answerId);
    }

    public AnswerNotFoundException(String errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }
}
