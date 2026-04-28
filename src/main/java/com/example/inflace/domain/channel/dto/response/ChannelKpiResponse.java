package com.example.inflace.domain.channel.dto.response;

public record ChannelKpiResponse(
        Long totalViews,
        Double avgEngagementRate,
        Double avgRetentionRate,
        Double weeklyUploadCount
) {
    public static ChannelKpiResponse from(Long totalViews, Double avgEngagementRate, Double avgRetentionRate,
                                          Double weeklyUploadCount
    ) {
        return new ChannelKpiResponse(
                totalViews != null ? totalViews : 0L,
                avgEngagementRate != null ? avgEngagementRate : 0.0,
                avgRetentionRate != null ? avgRetentionRate : 0.0,
                weeklyUploadCount != null ? weeklyUploadCount : 0.0
        );
    }
}
