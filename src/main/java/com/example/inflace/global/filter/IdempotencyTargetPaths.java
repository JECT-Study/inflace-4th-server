package com.example.inflace.global.filter;

public final class IdempotencyTargetPaths {

    private static final String[] TARGET_GET_PATHS = {
            "/api/v1/influencers/*/insight-summary"
    };

    private IdempotencyTargetPaths() {
    }

    public static String[] targetGetPaths() {
        return TARGET_GET_PATHS.clone();
    }
}
