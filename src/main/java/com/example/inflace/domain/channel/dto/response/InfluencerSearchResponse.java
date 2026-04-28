package com.example.inflace.domain.channel.dto.response;

import java.util.List;

public record InfluencerSearchResponse(
        Long channelId,
        String channelName,
        String channelHandle,
        String thumbnailUrl,
        List<String> categories,
        Long subscriberCount,
        Double averageEngagementRate,
        Double averageViews,
        Integer recentUploadCount30d
) {
}
