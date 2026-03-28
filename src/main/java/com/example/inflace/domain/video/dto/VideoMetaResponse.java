package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.Video;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record VideoMetaResponse(
        String thumbnailUrl,
        String videoUrl,
        String title,
        LocalDateTime publishedAt,
        String description,
        List<String> hashtags
) {
    public static VideoMetaResponse from(Video video) {
        return new VideoMetaResponse(
                video.getThumbnailUrl(),
                video.getVideoUrl(),
                video.getTitle(),
                video.getPublishedAt(),
                video.getDescription(),
                Arrays.stream(video.getHashtags()).toList()
        );
    }
}
