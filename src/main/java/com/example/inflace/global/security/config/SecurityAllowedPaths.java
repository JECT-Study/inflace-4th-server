package com.example.inflace.global.security.config;

public final class SecurityAllowedPaths {

    private static final String[] ALLOWED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/health-check",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/swagger-ui/**"
    };

    private SecurityAllowedPaths() {
    }

    public static String[] allowedPaths() {
        return ALLOWED_PATHS.clone();
    }
}
