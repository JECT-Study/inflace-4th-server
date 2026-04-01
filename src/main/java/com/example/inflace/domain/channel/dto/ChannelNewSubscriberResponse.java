package com.example.inflace.domain.channel.dto;

import java.util.List;

public record ChannelNewSubscriberResponse(
        List<NewSubscriberVideo> videos
) {
    public record NewSubscriberVideo(
            int rank,
            Long videoId,
            String title,
            String thumbnailUrl,
            Long viewCount,
            Long subscriptionConversionCount,
            Double newSubscriberRatio,
            Double retentionRate
    ) {
    }
}
