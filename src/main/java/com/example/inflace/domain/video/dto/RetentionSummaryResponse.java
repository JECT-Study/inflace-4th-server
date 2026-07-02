package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;

import static com.example.inflace.global.util.AnalyticsParser.safeDoubleValue;

public record RetentionSummaryResponse(RetentionData retentionData) {

    public record RetentionData(
            Double avgWatchDuration,
            Double relativeRetentionAvg
    ) {
    }

    public static RetentionSummaryResponse from(VideoStats stats, VideoAnalytics analytics) {
        return new RetentionSummaryResponse(
                new RetentionData(
                        safeDoubleValue(analytics != null ? analytics.getAvgWatchDuration() : null),
                        safeDoubleValue(analytics != null ? analytics.getRelativeRetentionPerformance() : null)
                )
        );
    }
}
