package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.VideoStats;

public record RetentionSummaryResponse(RetentionData retentionData) {

    public record RetentionData(
            Double avgWatchDuration,
            Double relativeRetentionAvg
    ) {
    }

    public static RetentionSummaryResponse from(VideoStats stats) {
        return new RetentionSummaryResponse(
                new RetentionData(
                        stats.getAvgWatchDuration(),
                        stats.getRelativeRetentionPerformance()
                )
        );
    }
}
