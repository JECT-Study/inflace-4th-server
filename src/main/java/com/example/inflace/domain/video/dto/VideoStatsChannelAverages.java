package com.example.inflace.domain.video.dto;

public record VideoStatsChannelAverages(
        Double viewCount,
        Double likeCount,
        Double commentCount,
        Double shareCount,
        Double subscribersGained,
        Double avd,
        Double engagementRate,
        Double newViewerRate,
        Double outlier,
        Double vph
) {
}
