package com.example.inflace.domain.channel.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record YoutubeDataChannelResponse(
        List<Item> items
) {
    public record Item(
            String id,
            Snippet snippet,
            Statistics statistics,
            ContentDetails contentDetails
    ){}
    public record Snippet(
            String title,
            String description,
            String customUrl,
            String publishedAt,
            String country,
            Thumbnails thumbnails
    ){}
    public record Thumbnails(
            @JsonProperty("default") Thumbnail defaultThumbnail,
            Thumbnail medium,
            Thumbnail high
    ) {}
    public record Thumbnail(
            String url
    ) {}
    public record Statistics(
       String subscriberCount,
       String viewCount,
       String videoCount
    ){}
    public record ContentDetails(
            RelatedPlaylists relatedPlaylists
    ) {}
    public record RelatedPlaylists(
            String uploads
    ) {}
}
