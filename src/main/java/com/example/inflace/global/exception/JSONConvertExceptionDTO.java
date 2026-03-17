package com.example.inflace.global.exception;

import com.example.inflace.global.response.ExceptionResponse;
import lombok.Getter;
import org.springframework.http.converter.HttpMessageConversionException;

import java.util.Optional;

@Getter
public class JSONConvertExceptionDTO extends ExceptionResponse {
    private final String message;
    private final String cause;

    public JSONConvertExceptionDTO(HttpMessageConversionException jsonException) {
        super(ErrorDefine.INVALID_ARGUMENT);

        this.message = jsonException.getMessage();
        this.cause = Optional.ofNullable(jsonException.getCause())
                .map(Throwable::toString)
                .orElse("Non-Throwable Cause");
    }
}