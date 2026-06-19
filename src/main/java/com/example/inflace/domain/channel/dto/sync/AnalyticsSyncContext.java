package com.example.inflace.domain.channel.dto.sync;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AnalyticsSyncContext(
        Long channelId,
        String youtubeChannelId,
        LocalDateTime youtubePublishedAt,
        Long subscriberCount,
        LocalDate latestSubscriberLogDate,
        List<VideoContext> videos
) {

    public record VideoContext(
            Long videoId,
            String youtubeVideoId,
            LocalDateTime publishedAt,
            boolean existingVideoAnalytics,
            Long viewCount
    ) {
    }
}
