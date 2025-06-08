package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

/**
 * 질문 생성 실패 예외
 */
public class QuestionGenerationException extends LLMServiceException {

    public QuestionGenerationException() {
        super("error.llm.question.generation.failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public QuestionGenerationException(String errorCode) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

