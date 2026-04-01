package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
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
        public static NewSubscriberVideo from(int rank, Video video, VideoStats stats) {
            return new NewSubscriberVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    stats != null ? stats.getViewCount() : 0L,
                    stats != null ? stats.getSubscribersGained() : 0L,
                    stats != null && stats.getUnsubscribedViewerPercentage() != null
                            ? stats.getUnsubscribedViewerPercentage() : 0.0,
                    stats != null && stats.getAverageViewPercentage() != null
                            ? stats.getAverageViewPercentage() : 0.0
            );
        }
    }
}
