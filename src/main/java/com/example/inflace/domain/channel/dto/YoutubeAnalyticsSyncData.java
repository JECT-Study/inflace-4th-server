package com.example.inflace.domain.channel.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record YoutubeAnalyticsSyncData(
        ChannelAnalyticsData channelAnalytics,
        List<SubscriberLogData> subscriberLogs,
        List<VideoAnalyticsData> videoAnalytics,
        List<AudienceRetentionData> audienceRetentions
) {
    public record ChannelAnalyticsData(
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime collectedAt,
            Long views,
            Long subscriberViewCount,
            Long nonSubscriberViewCount,
            Long watchedMinutes,
            Integer averageViewDurationSeconds,
            Map<String, Double> audienceGender,
            Map<String, Double> audienceAge,
            Map<String, Double> audienceCountry
    ) {
    }

    public record SubscriberLogData(
            LocalDate recordedDate,
            Long subscriberCount,
            Long subscribersGained,
            Long subscribersLost
    ) {
    }

    public record VideoAnalyticsData(
            Long videoId,
            boolean updateSummary,
            Long shareCount,
            Long subscribersGained,
            Double ctr,
            Double avgWatchDuration,
            Double averageViewPercentage,
            LocalDateTime collectedAt,
            Long unsubscribedViewCount,
            Double unsubscribedViewerPercentage
    ) {
    }

    public record AudienceRetentionData(
            Long videoId,
            List<AudienceRetentionPointData> retentions,
            Double relativeRetentionPerformance
    ) {
    }

    public record AudienceRetentionPointData(
            Double timeRatio,
            Double retentionRate,
            LocalDateTime collectedAt
    ) {
    }
}
