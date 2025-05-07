package dev.swote.interv.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String entityName, String fieldName, Object fieldValue) {
        super("error.resource.notFound", HttpStatus.NOT_FOUND);
    }
}