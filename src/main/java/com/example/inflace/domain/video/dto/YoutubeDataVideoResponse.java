package com.example.inflace.domain.video.dto;

import java.util.List;

public record YoutubeDataVideoResponse(
        List<Item> items
) {
    public record Item(
            String id,
            Snippet snippet,
            ContentDetails contentDetails,
            Statistics statistics
    ) {
    }

    public record Snippet(
            String channelId,
            String title,
            String publishedAt,
            String description,
            Thumbnails thumbnails,
            List<String> tags,
            String categoryId
    ) {
    }

    public record Thumbnails(
            Thumbnail high
    ) {
    }

    public record Thumbnail(
            String url
    ) {
    }

    public record ContentDetails(
            String duration  // 쇼츠 판별용
    ) {
    }

    public record Statistics(
            String viewCount,
            String likeCount,
            String commentCount
    ) {
    }
}
