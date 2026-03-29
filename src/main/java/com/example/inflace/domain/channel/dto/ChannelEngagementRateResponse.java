package com.example.inflace.domain.channel.dto;

import java.util.List;

public record ChannelEngagementRateResponse(
        Summary summary,
        List<EngageVideo> videos
) {
    public record Summary(
            Double longFormAverageEngagementRate,
            Double shortFormAverageEngagementRate
    ) {
    }

    public record EngageVideo(
            int rank,
            Long videoId,
            String title,
            String thumbnailUrl,
            String contentType,
            Double engagementRate
    ) {
    }
}
