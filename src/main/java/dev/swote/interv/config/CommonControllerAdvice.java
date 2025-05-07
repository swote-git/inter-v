package dev.swote.interv.config;

import dev.swote.interv.util.MessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.filters.AddDefaultCharsetFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice {
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<AddDefaultCharsetFilter.ResponseWrapper> baseExceptionHandler(BaseException e) {
        log.error("On ControllerAdvice: ", e);

        return ResponseEntity.status(e.getHttpStatus())
                .body(
                        ResponseWrapper.builder()
                                .message(e.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public ResponseEntity<AddDefaultCharsetFilter.ResponseWrapper> NoSuchAlgorithmExceptionHandler(){
        log.error("Server settings errors: In the com.pub.data.util.security.PasswordCoder, "
                + "check for the MessageDigest.getInstance parameter in the hash method");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ResponseWrapper.builder()
                                .message(MessageConverter.getMessage("error.common.internalServerErrors"))
                                .build()
                );
    }

    @ExceptionHandler(GuidelinesNotFoundException.class)
    public ResponseEntity<AddDefaultCharsetFilter.ResponseWrapper> GuidelinesNotFoundExceptionHandler(GuidelinesNotFoundException e){
        final ResponseEntity<AddDefaultCharsetFilter.ResponseWrapper> response = baseExceptionHandler(e);
        log.error("Not Found Guideline with: " + e.getPointTypeCode().getCodeName());

        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptionHandler(MethodArgumentNotValidException ex){
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(c -> errors.put(((FieldError) c).getField(), c.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<Map<String, String>> UnexpectedTypeExceptionHandler(MethodArgumentNotValidException ex){
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(c -> errors.put(((FieldError) c).getField(), c.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

}