package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.*;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;

    public VideoMetaResponse getVideoMeta(Long videoId) {
        // 영상 목록에서 클릭 후 이동, 외부 API 필요하지 않음
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        return VideoMetaResponse.from(video);
    }

    public VideoStatsResponse getVideoStats(String googleId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        VideoStats videoStats = videoStatsRepository.findByVideoId(videoId)
                .orElseGet(() -> {
                    YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                            video.getPublishedAt().toLocalDate(),
                            LocalDateTime.now().toLocalDate(),
                            List.of("views", "likes", "comments", "shares", "subscribersGained",
                                    "averageViewDuration"), // videoThumbnailImpressionsClickRate 우선 제외
                            video.getYoutubeVideoId()
                    );

                    YoutubeAnalyticsVideoResponse response = getYoutubeAnalyticsVideo(googleId, request);

                    List<String> columns = response.columnHeaders().stream()
                            .map(YoutubeAnalyticsVideoResponse.ColumnHeader::name)
                            .toList(); // 순서 기반 배열 매핑

                    if (response.rows() == null || response.rows().isEmpty()) {
                        // TODO: 정책 결정 전까지 빈 stats 저장
                        return videoStatsRepository.save(VideoStats.builder()
                                .video(video)
                                .viewCount(0L)
                                .likeCount(0L)
                                .commentCount(0L)
                                .shareCount(0L)
                                .subscribersGained(0L)
                                .ctr(0.0)
                                .avgWatchDuration(0.0)
                                .collectedAt(LocalDateTime.now())
                                .build());
                    }

                    List<Object> row = response.rows().get(0);

                    return videoStatsRepository.save(VideoStats.builder()
                            .video(video)
                            .viewCount(toLong(row.get(columns.indexOf("views"))))
                            .likeCount(toLong(row.get(columns.indexOf("likes"))))
                            .commentCount(toLong(row.get(columns.indexOf("comments"))))
                            .shareCount(toLong(row.get(columns.indexOf("shares"))))
                            .subscribersGained(toLong(row.get(columns.indexOf("subscribersGained"))))
//                            .ctr(toDouble(row.get(columns.indexOf("videoThumbnailImpressionsClickRate"))))
                            .avgWatchDuration(toDouble(row.get(columns.indexOf("averageViewDuration"))))
                            .collectedAt(LocalDateTime.now())
                            .build());
                });

        return VideoStatsResponse.from(videoStats, 0L, 0L);
    }

    private YoutubeDataVideoResponse getYoutubeDataVideo(String videoId, String parts) {
        return youtubeDataApiClient.getYoutubeVideo(videoId, parts);
    }

    private YoutubeAnalyticsVideoResponse getYoutubeAnalyticsVideo(String googleId,
                                                                   YoutubeAnalyticsVideoRequest request) {
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        return response;
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        return ((Number) value).doubleValue();
    }
}
