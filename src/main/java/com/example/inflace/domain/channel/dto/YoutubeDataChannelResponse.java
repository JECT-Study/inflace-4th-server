package com.example.inflace.domain.channel.dto;

import java.util.List;

public record YoutubeDataChannelResponse(
        List<Item> items
) {
    public record Item(
            String id,
            Snippet snippet,
            Statistics statistics
    ){}
    public record Snippet(
            String title,
            String publishedAt,
            String country
    ){}
    public record Statistics(
       String subscriberCount,
       String viewCount,
       String videoCount
    ){}
}
