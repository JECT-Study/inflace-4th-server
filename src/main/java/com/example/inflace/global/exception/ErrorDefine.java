package com.example.inflace.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefine {

    INVALID_HEADER_ERROR("AUTH_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Header Error"),
    INVALID_ARGUMENT("COMMON_400", HttpStatus.BAD_REQUEST, "Bad Request: Invalid Arguments"),
    INVALID_DATE_FORMAT("COMMON_400_DATE_FORMAT", HttpStatus.BAD_REQUEST, "Bad Request: Invalid date format"),
    INVALID_DATE_RANGE("COMMON_400_DATE_RANGE", HttpStatus.BAD_REQUEST, "Bad Request: Invalid date range"),
    DUPLICATE_IDEMPOTENCY_REQUEST("COMMON_409_IDEMPOTENCY", HttpStatus.CONFLICT, "Conflict: Duplicate idempotency request"),
    INTERNAL_SERVER_ERROR("500", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    AUTH_UNSUPPORTED_PROVIDER("AUTH_401", HttpStatus.BAD_REQUEST, "Bad Request: Unsupported OAuth Provider"),
    AUTHENTICATION_FAILED("AUTH_401_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Unauthorized: Authentication required"),
    INVALID_ACCESS_TOKEN("AUTH_401_ACCESS", HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid access token"),
    LOGGED_OUT_ACCESS_TOKEN("AUTH_401_LOGOUT", HttpStatus.UNAUTHORIZED, "Unauthorized: Logged out access token"),
    AUTH_FORBIDDEN("AUTH_403", HttpStatus.FORBIDDEN, "Forbidden: No permission to access this resource"),
    INVALID_REFRESH_TOKEN("AUTH_401_REFRESH", HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid or expired refresh token"),
    REFRESH_TOKEN_NOT_FOUND("AUTH_401_MISSING", HttpStatus.UNAUTHORIZED, "Unauthorized: Refresh token not found"),

    // USER
    USER_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "Not Found: User not found"),
    ONBOARDING_INVALID_REQUEST("USER_400", HttpStatus.BAD_REQUEST, "Bad Request: Role and need are required"),
    USER_ALREADY_DELETED("USER_404_DELETED", HttpStatus.NOT_FOUND, "Not Found: User already deleted"),
    INVALID_PROFILE_IMAGE("USER_400_PROFILE_IMAGE", HttpStatus.BAD_REQUEST, "Bad Request: Invalid profile image"),
    PROFILE_IMAGE_UPLOAD_FAILED("USER_500_PROFILE_IMAGE_UPLOAD", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: Failed to upload profile image"),

    // VIDEO
    VIDEO_NOT_FOUND("VIDEO_404", HttpStatus.NOT_FOUND, "Not Found: Video not found"),
    VIDEO_STATS_NOT_FOUND("VIDEO_STATS_404", HttpStatus.NOT_FOUND, "Not Found: Video Stats not found"),
    // RETENTION_NOT_FOUND("RETENTION_404", HttpStatus.NOT_FOUND, "Not Found: Retention not found"),
    RETENTION_INVALID("RETENTION_400", HttpStatus.BAD_REQUEST, "Bad Request: Retention data must have exactly 100 points"),

    //CHANNEL
    CHANNEL_NOT_FOUND("CHANNEL_404", HttpStatus.NOT_FOUND, "Not Found: Channel Not Found"),
    CHANNEL_STATS_NOT_FOUND("CHANNEL_STATS_404", HttpStatus.NOT_FOUND, " Not Found: Channel Stats not found"),
    CHANNEL_ANALYTICS_NOT_FOUND("CHANNEL_ANALYTICS_404", HttpStatus.NOT_FOUND, "Not Found: Channel Analytics not found"),
    CHANNEL_SYNC_COOLDOWN("CHANNEL_429_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests: Channel sync is currently on cooldown"),
    CHANNEL_INSIGHT_REQUIRES_MIN_VIDEO_COUNT("CHANNEL_INSIGHT_400", HttpStatus.BAD_REQUEST, "Bad Request: At least 50 videos are required for channel insight"),
    ANALYTICS_DATA_NOT_FOUND("ANALYTICS_404", HttpStatus.NOT_FOUND, "Not Found: Analytics data not found"),

    //ETC
    EXTERNAL_API_RATE_LIMITED("EXTERNAL_429", HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests: External API capacity is temporarily limited"),
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
