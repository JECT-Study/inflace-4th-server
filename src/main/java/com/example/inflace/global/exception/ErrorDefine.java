package com.example.inflace.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefine {

    INVALID_HEADER_ERROR("AUTH_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Header Error"),
    INVALID_ARGUMENT("COMMON_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Arguments"),
    AUTH_UNSUPPORTED_PROVIDER("AUTH_401", HttpStatus.BAD_REQUEST, "Bad Request: Unsupported OAuth Provider"),
    AUTH_FORBIDDEN("AUTH_403", HttpStatus.FORBIDDEN, "Forbidden: No permission to access this resource"),

    // VIDEO
    VIDEO_NOT_FOUND("VIDEO_404", HttpStatus.NOT_FOUND, "Not Found: Video not found"),
    VIDEO_STATS_NOT_FOUND("VIDEO_STATS_404", HttpStatus.NOT_FOUND, "Not Found: Video Stats not found"),
    RETENTION_NOT_FOUND("RETENTION_404", HttpStatus.NOT_FOUND, "Not Found: Retention not found"),
    RETENTION_INVALID("RETENTION_400", HttpStatus.BAD_REQUEST, "Bad Request: Retention data must have exactly 100 points"),

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
