package com.example.inflace.global.exception;

import com.example.inflace.global.response.ExceptionResponse;
import lombok.Getter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class InvalidateArgumentExceptionDTO extends ExceptionResponse {
    private final Map<String, String> errorFields;

    public InvalidateArgumentExceptionDTO(MethodArgumentNotValidException invalidException) {
        super(ErrorDefine.INVALID_ARGUMENT);

        this.errorFields = invalidException.getBindingResult().getFieldErrors()
                .stream().collect(Collectors
                        .toMap(FieldError::getField, FieldError::getDefaultMessage));
    }
}