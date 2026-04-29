package com.example.inflace.domain.channel.dto.response;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
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
        public static NewSubscriberVideo from(int rank, Video video, VideoStats stats, VideoAnalytics analytics) {
            return new NewSubscriberVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    stats != null ? stats.getViewCount() : 0L,
                    analytics != null && analytics.getSubscribersGained() != null
                            ? analytics.getSubscribersGained() : 0L,
                    analytics != null && analytics.getUnsubscribedViewerPercentage() != null
                            ? analytics.getUnsubscribedViewerPercentage() : 0.0,
                    analytics != null && analytics.getAverageViewPercentage() != null
                            ? analytics.getAverageViewPercentage() : 0.0
            );
        }
    }
}
