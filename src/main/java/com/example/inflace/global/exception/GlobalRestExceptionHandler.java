package com.example.inflace.global.exception;

import com.example.inflace.global.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.CompletionException;

@RestControllerAdvice
public class GlobalRestExceptionHandler {
    @ExceptionHandler(value = {ApiException.class})
    public ResponseEntity<?> handleApiException(ApiException e) {
        return BaseResponse.toResponseEntity(e);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<?> handleInvalidArgumentException(MethodArgumentNotValidException e) {
        return BaseResponse.toResponseEntity(e);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<Object> handleJSONConversionException(HttpMessageConversionException e) {
        return BaseResponse.toResponseEntity(e);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<?> handleException(Exception e) {
        return BaseResponse.toResponseEntity(e);
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<?> handleCompletionException(CompletionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ApiException apiException) {
            return BaseResponse.toResponseEntity(apiException);
        }

        return BaseResponse.toResponseEntity(e);
    }

}