package com.example.inflace.domain.channel.dto.response;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.global.util.AnalyticsCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ChannelTopMainVideosResponse(
        List<ChannelTopMainVideo> videos
) {
    public record ChannelTopMainVideo(
            Long rank,
            Long videoId,
            String title,
            String thumbnailUrl,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            Double engagementRate,
            LocalDateTime publishedAt,
            Double ctr
    ) {
        public static ChannelTopMainVideo of(long rank, Video video, VideoStats stats, VideoAnalytics analytics) {
            long viewCount = 0L;
            long likeCount = 0L;
            long commentCount = 0L;
            double ctr = 0.0;
            double engagementRate = 0.0;

            if (stats != null) {
                viewCount = Objects.requireNonNullElse(stats.getViewCount(), 0L);
                likeCount = Objects.requireNonNullElse(stats.getLikeCount(), 0L);
                commentCount = Objects.requireNonNullElse(stats.getCommentCount(), 0L);
                engagementRate = AnalyticsCalculator.engagementRate(
                        stats.getLikeCount(), stats.getCommentCount(), stats.getViewCount());
            }
            if (analytics != null) {
                ctr = Objects.requireNonNullElse(analytics.getCtr(), 0.0);
            }

            return new ChannelTopMainVideo(rank, video.getId(), video.getTitle(), video.getThumbnailUrl(),
                    viewCount, likeCount, commentCount, engagementRate, video.getPublishedAt(), ctr);
        }
    }
}
