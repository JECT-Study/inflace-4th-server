package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;

public record RetentionSummaryResponse(RetentionData retentionData) {

    public record RetentionData(
            Double avgWatchDuration,
            Double relativeRetentionAvg
    ) {
    }

    public static RetentionSummaryResponse from(VideoStats stats, VideoAnalytics analytics) {
        return new RetentionSummaryResponse(
                new RetentionData(
                        analytics != null ? analytics.getAvgWatchDuration() : null,
                        analytics != null ? analytics.getRelativeRetentionPerformance() : null
                )
        );
    }
}
