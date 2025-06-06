package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class QuestionIndexOutOfBoundsException extends BaseException {

    public QuestionIndexOutOfBoundsException() {
        super("error.question.index.out.of.bounds", HttpStatus.BAD_REQUEST);
    }

    public QuestionIndexOutOfBoundsException(String message) {
        super("error.question.index.out.of.bounds", HttpStatus.BAD_REQUEST, message);
    }

    public QuestionIndexOutOfBoundsException(Integer currentIndex, Integer totalQuestions) {
        super("error.question.index.out.of.bounds", HttpStatus.BAD_REQUEST,
                String.format("Question index %d is out of bounds. Total questions: %d", currentIndex, totalQuestions));
    }
}