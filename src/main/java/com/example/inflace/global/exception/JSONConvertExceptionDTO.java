package com.example.inflace.global.exception;

import com.example.inflace.global.response.ExceptionResponse;
import org.springframework.http.converter.HttpMessageConversionException;

public class JSONConvertExceptionDTO extends ExceptionResponse {
    public JSONConvertExceptionDTO(HttpMessageConversionException jsonException) {
        super(ErrorDefine.INVALID_ARGUMENT);
    }
}
