package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.global.service.YoutubeAnalyticsService;
import com.example.inflace.global.util.AnalyticsParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoStatsApiService {

    private final YoutubeAnalyticsService youtubeAnalyticsService;

    public VideoStats fetchStats(Video video, String googleId) {
        YoutubeAnalyticsVideoRequest request = buildRequest(video);
        Map<String, Object> data = youtubeAnalyticsService.query(googleId, request);

        if (data.isEmpty()) {
            // TODO: 정책 결정 전까지 빈 stats return
            return emptyVideoStats(video);
        }

        return VideoStats.builder()
                .video(video)
                .viewCount(AnalyticsParser.toLong(data.get("views")))
                .likeCount(AnalyticsParser.toLong(data.get("likes")))
                .commentCount(AnalyticsParser.toLong(data.get("comments")))
                .shareCount(AnalyticsParser.toLong(data.get("shares")))
                .subscribersGained(AnalyticsParser.toLong(data.get("subscribersGained")))
                .avgWatchDuration(AnalyticsParser.toDouble(data.get("averageViewDuration")))
                .collectedAt(LocalDateTime.now())
                .build();
    }

    private VideoStats emptyVideoStats(Video video) {
        return VideoStats.builder()
                .video(video)
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .subscribersGained(0L)
                .ctr(0.0)
                .avgWatchDuration(0.0)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    // videoStats 요청 조합
    private YoutubeAnalyticsVideoRequest buildRequest(Video video) {
        return new YoutubeAnalyticsVideoRequest(
                video.getPublishedAt().toLocalDate(),
                LocalDateTime.now().toLocalDate(),
                List.of("views", "likes", "comments", "shares", "subscribersGained",
                        "averageViewDuration"), // videoThumbnailImpressionsClickRate 우선 제외
                video.getYoutubeVideoId()
        );
    }
}
