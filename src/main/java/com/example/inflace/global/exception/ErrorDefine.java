package com.example.inflace.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefine {

    INVALID_HEADER_ERROR("AUTH_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Header Error"),
    INVALID_ARGUMENT("COMMON_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Arguments"),
    AUTH_UNSUPPORTED_PROVIDER("AUTH_401", HttpStatus.BAD_REQUEST, "Bad Request: Unsupported OAuth Provider"),
    AUTH_FORBIDDEN("AUTH_403", HttpStatus.FORBIDDEN, "Forbidden: No permission to access this resource"),
    INVALID_REFRESH_TOKEN("AUTH_401_REFRESH", HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid or expired refresh token"),
    REFRESH_TOKEN_NOT_FOUND("AUTH_401_MISSING", HttpStatus.UNAUTHORIZED, "Unauthorized: Refresh token not found"),

    // USER
    USER_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "Not Found: User not found"),
    ONBOARDING_INVALID_REQUEST("USER_400", HttpStatus.BAD_REQUEST, "Bad Request: Role and need are required"),
    USER_ALREADY_DELETED("USER_404_DELETED", HttpStatus.NOT_FOUND, "Not Found: User already deleted"),

    // VIDEO
    VIDEO_NOT_FOUND("VIDEO_404", HttpStatus.NOT_FOUND, "Not Found: Video not found"),
    VIDEO_STATS_NOT_FOUND("VIDEO_STATS_404", HttpStatus.NOT_FOUND, "Not Found: Video Stats not found"),
    RETENTION_NOT_FOUND("RETENTION_404", HttpStatus.NOT_FOUND, "Not Found: Retention not found"),
    RETENTION_INVALID("RETENTION_400", HttpStatus.BAD_REQUEST, "Bad Request: Retention data must have exactly 100 points"),

    //CHANNEL
    CHANNEL_NOT_FOUND("CHANNEL_404", HttpStatus.NOT_FOUND, "Not Found: Channel Not Found"),
    CHANNEL_STATS_NOT_FOUND("CHANNEL_STATS_404", HttpStatus.NOT_FOUND, " Not Found: Channel Stats not found"),

    //ETC
    YOUTUBE_TOKEN_NOT_FOUND("YOUTUBE_401", HttpStatus.UNAUTHORIZED, "Unauthorized: YouTube access token not found. Please link your YouTube account first"),
    YOUTUBE_CHANNEL_NOT_FOUND("YOUTUBE_404", HttpStatus.NOT_FOUND, "Not Found: YouTube channel not found"),
    YOUTUBE_API_ERROR("YOUTUBE_500", HttpStatus.INTERNAL_SERVER_ERROR, "YouTube API Error");

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorDefine(String errorCode, HttpStatus httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
