package com.example.inflace.domain.channel.dto.response;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import java.util.List;

public record ChannelTopVideosResponse(
        List<ChannelTopVideo> videos
) {
    public record ChannelTopVideo(
            Integer rank,
            Long videoId,
            String title,
            String thumbnailUrl,
            Long viewCount,
            Double engagementRate,
            Double ctr,
            Double retentionRate
    ){
        public static ChannelTopVideo from(int rank, Video video, VideoStats videoStats, VideoAnalytics videoAnalytics) {
            return new ChannelTopVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    videoStats != null ? videoStats.getViewCount() : 0L,
                    videoStats != null && videoStats.getRisingScore() != null ? videoStats.getRisingScore() : 0.0,
                    videoAnalytics != null && videoAnalytics.getCtr() != null ? videoAnalytics.getCtr() : 0.0,
                    videoAnalytics != null && videoAnalytics.getAverageViewPercentage() != null
                            ? videoAnalytics.getAverageViewPercentage() : 0.0
            );
        }
    }
}
