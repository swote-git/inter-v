package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException() {
        super("error.user.not.found", HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException(Integer userId) {
        super("error.user.not.found", HttpStatus.NOT_FOUND, "User not found with id: " + userId);
    }

    public UserNotFoundException(String errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }
}