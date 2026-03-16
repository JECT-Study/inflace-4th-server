package com.example.inflace.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefine {
    
    INVALID_HEADER_ERROR("4006", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Header Error");


    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorDefine(String errorCode, HttpStatus httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}