package com.example.inflace.domain.video.repository.projection;

public record VideoFormatStatsProjection(
        boolean isShort,
        long viewCount,
        long likeCount,
        long commentCount
) {
}
