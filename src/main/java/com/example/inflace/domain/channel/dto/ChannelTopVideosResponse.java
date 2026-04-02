package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.video.domain.Video;
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
        public static ChannelTopVideo from(int rank, Video video, VideoStats videoStats) {
            return new ChannelTopVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    videoStats != null ? videoStats.getViewCount() : 0L,
                    video.getRisingScore() != null ? video.getRisingScore() : 0.0,
                    videoStats != null && videoStats.getCtr() != null ? videoStats.getCtr() : 0.0,
                    videoStats != null && videoStats.getAverageViewPercentage() != null
                            ? videoStats.getAverageViewPercentage() : 0.0
            );
        }
    }
}
