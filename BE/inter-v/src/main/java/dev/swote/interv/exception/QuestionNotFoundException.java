package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class QuestionNotFoundException extends BaseException {

    public QuestionNotFoundException() {
        super("error.question.not.found", HttpStatus.NOT_FOUND);
    }

    public QuestionNotFoundException(Integer questionId) {
        super("error.question.not.found", HttpStatus.NOT_FOUND, "Question not found with id: " + questionId);
    }

    public QuestionNotFoundException(String errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }

    public QuestionNotFoundException(String errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}