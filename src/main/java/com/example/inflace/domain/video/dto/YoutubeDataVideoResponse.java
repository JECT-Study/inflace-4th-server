package com.example.inflace.domain.video.dto;

import java.util.List;

public record YoutubeDataVideoResponse(
        List<Item> items
) {
    public record Item(
            String id,
            Snippet snippet,
            ContentDetails contentDetails
    ) {
    }

    public record Snippet(
            String title,
            String publishedAt,
            String description,
            Thumbnails thumbnails,
            List<String> tags
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
}
