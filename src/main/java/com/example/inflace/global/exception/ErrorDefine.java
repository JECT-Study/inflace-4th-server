package com.example.inflace.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefine {

    INVALID_HEADER_ERROR("AUTH_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Header Error"),
    INVALID_ARGUMENT("COMMON_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Arguments"),
    AUTH_UNSUPPORTED_PROVIDER("AUTH_401", HttpStatus.BAD_REQUEST, "Bad Request: Unsupported OAuth Provider"),

    // VIDEO
    VIDEO_NOT_FOUND("VIDEO_404", HttpStatus.NOT_FOUND, "Not Found: Video not found");
    CHANNEL_NOT_FOUND("CHANNEL_404", HttpStatus.NOT_FOUND, "Not Found: Channel Not Found");

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorDefine(String errorCode, HttpStatus httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}