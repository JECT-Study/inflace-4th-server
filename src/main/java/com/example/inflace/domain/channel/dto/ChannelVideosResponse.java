package com.example.inflace.domain.channel.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChannelVideosResponse(
        List<ChannelVideoItem> videos,
        PageInfo pageInfo
) {
    public record ChannelVideoItem(
            Long videoId,
            String title,
            String thumbnailUrl,
            LocalDateTime publishedAt,
            Long viewCount,
            Long likeCount,
            Long commentCount
    ) {
    }

    public record PageInfo(
            Integer size,
            String nextCursor,
            Boolean hasNext
    ) {
    }
}
