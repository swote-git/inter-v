package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class InterviewSessionNotFoundException extends BaseException {

    public InterviewSessionNotFoundException() {
        super("error.interview.session.not.found", HttpStatus.NOT_FOUND);
    }

    public InterviewSessionNotFoundException(Integer interviewId) {
        super("error.interview.session.not.found", HttpStatus.NOT_FOUND, "Interview session not found with id: " + interviewId);
    }

    public InterviewSessionNotFoundException(String shareUrl) {
        super("error.interview.session.not.found", HttpStatus.NOT_FOUND, "Interview session not found with share URL: " + shareUrl);
    }

    public InterviewSessionNotFoundException(String errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}