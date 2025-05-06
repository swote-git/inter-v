package dev.swote.interv.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class CommonResponse<T> {
    private int status;
    private T data;
    private String message;

    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>(HttpStatus.OK.value(), data, null);
    }
}