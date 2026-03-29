package com.example.inflace.global.util;

/**
 * Analytics parsing에서 사용하는 함수
 */
public class AnalyticsParser {
    public static Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    public static Double toDouble(Object value) {
        if (value == null) return 0.0;
        return ((Number) value).doubleValue();
    }

    public static Double safeDoubleValue(Long value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
