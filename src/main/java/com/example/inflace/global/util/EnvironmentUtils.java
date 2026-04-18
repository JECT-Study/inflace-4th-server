package com.example.inflace.global.util;

import org.springframework.core.env.Environment;

import java.util.Arrays;

public final class EnvironmentUtils {

    private EnvironmentUtils() {
    }

    public static boolean isLocal(Environment environment) {
        return hasProfile(environment, "local");
    }

    public static boolean isTest(Environment environment) {
        return hasProfile(environment, "test");
    }

    public static boolean isProd(Environment environment) {
        return hasProfile(environment, "prod");
    }

    public static boolean isDev(Environment environment) {
        return hasProfile(environment, "dev");
    }

    private static boolean hasProfile(Environment environment, String profile) {
        return Arrays.asList(environment.getActiveProfiles()).contains(profile);
    }
}
