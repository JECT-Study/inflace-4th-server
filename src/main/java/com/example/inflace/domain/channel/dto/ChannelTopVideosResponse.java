package com.example.inflace.domain.channel.dto;

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
    }
}
