package com.example.inflace.domain.channel.dto;

public record ChannelKpiResponse(
        Long totalViews,
        Double avgEngagementRate,
        Double avgViewDuration,
        Double weeklyUploadCount
) {
}
