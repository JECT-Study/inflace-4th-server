package com.example.inflace.domain.channel.dto;

import java.util.List;

public record YoutubeDataChannelResponse(
        List<Item> items
) {
    public record Item(
            String id,
            Snippet snippet,
            Statistics statistics,
            TopicDetails topicDetails
    ){}
    public record TopicDetails(
            List<String> topicCategories
    ){}
    public record Snippet(
            String title,
            String customUrl,
            String publishedAt,
            String country,
            Thumbnails thumbnails
    ){}
    public record Thumbnails(
            ThumbnailUrl high
    ){}
    public record ThumbnailUrl(
            String url
    ){}
    public record Statistics(
       String subscriberCount,
       String viewCount,
       String videoCount
    ){}
}
