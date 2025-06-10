package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String entityName, String fieldName, Object fieldValue) {
        super("error.resource.duplicate", HttpStatus.CONFLICT);
    }
    public DuplicateResourceException(String message) {
        super("error.resource.duplicate", HttpStatus.CONFLICT, message);
    }
}