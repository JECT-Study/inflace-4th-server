package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.channel.dto.enums.ChannelVideoSort;
import java.time.LocalDateTime;

public record ChannelVideoRow(
        Long videoId,
        String title,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        Double duration,
        Boolean isShort,
        Boolean isAd,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        Double ctr,
        Double vph,
        Double outlierScore

) {
    public ChannelVideosResponse.ChannelVideoItem toItem() {
        return new ChannelVideosResponse.ChannelVideoItem(
                videoId,
                title,
                thumbnailUrl,
                publishedAt,
                viewCount,
                likeCount,
                commentCount,
                duration,
                isShort,
                isAd
        );
    }

    public Object sortValue(ChannelVideoSort sort) {
        return switch (sort) {
            case LATEST -> publishedAt;
            case VIEWS -> viewCount == null ? 0L : viewCount;
            case LIKES -> likeCount == null ? 0L : likeCount;
            case VPH -> vph == null ? 0.0 : vph;
            case OUTLIER -> outlierScore == null ? 0.0 : outlierScore;
        };
    }
}