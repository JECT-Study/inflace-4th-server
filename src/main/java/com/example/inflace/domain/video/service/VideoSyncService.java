package com.example.inflace.domain.video.service;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import com.example.inflace.global.service.YoutubeAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.inflace.global.util.AnalyticsParser.toDouble;
import static com.example.inflace.global.util.AnalyticsParser.toLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSyncService {

    private static final List<String> STATS_METRICS = List.of(
            "views", "likes", "comments", "shares", "averageViewDuration", "averageViewPercentage", "subscribersGained", "annotationClickThroughRate"
    );

    private static final List<String> RETENTION_METRICS = List.of(
            "audienceWatchRatio", "relativeRetentionPerformance"
    );

    private static final List<String> UNSUBSCRIBED_METRICS = List.of(
            "views"
    );

    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;
    private final UserReadRepository userReadRepository;
    private final YoutubeAnalyticsService youtubeAnalyticsService;
    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;

    /**
     * 일반 stats 요청 로직
     * @param userId
     * @param videoId
     */
    @Transactional
    public void syncStats(long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = findVideoWithOwnership(userId, videoId);
        String googleId = getGoogleId(userId);

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                video.getCreatedAt().toLocalDate(),
                LocalDate.now().minusDays(3),
                STATS_METRICS,
                video.getYoutubeVideoId(),
                "video"
        );

        Map<String, Object> data = youtubeAnalyticsService.query(googleId, request);

        if (data.isEmpty()) {
            return;
        }

        Long viewCount = toLong(data.get("views"));
        Long likeCount = toLong(data.get("likes"));
        Long commentCount = toLong(data.get("comments"));
        Long shareCount = toLong(data.get("shares"));
        Long subscribersGained = toLong(data.get("subscribersGained"));
        Double avgWatchDuration = toDouble(data.get("averageViewDuration"));
        Double averageViewPercentage = toDouble(data.get("averageViewPercentage"));
        Double annotationClickThroughRate = toDouble(data.get("annotationClickThroughRate"));

        LocalDateTime now = LocalDateTime.now();

        Optional<VideoStats> existingStats = videoStatsRepository.findByVideoId(videoId);
        if (existingStats.isPresent()) {
            existingStats.get().update(viewCount, likeCount, commentCount, now);
        } else {
            videoStatsRepository.save(VideoStats.builder()
                    .video(video)
                    .viewCount(viewCount)
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .collectedAt(now)
                    .build());
        }

        Optional<VideoAnalytics> existingAnalytics = videoAnalyticsRepository.findByVideoId(videoId);
        if (existingAnalytics.isPresent()) {
            existingAnalytics.get().update(
                    shareCount,
                    subscribersGained,
                    annotationClickThroughRate,
                    avgWatchDuration,
                    averageViewPercentage,
                    now
            );
        } else {
            videoAnalyticsRepository.save(VideoAnalytics.builder()
                    .video(video)
                    .shareCount(shareCount)
                    .subscribersGained(subscribersGained)
                    .ctr(annotationClickThroughRate)
                    .avgWatchDuration(avgWatchDuration)
                    .averageViewPercentage(averageViewPercentage)
                    .collectedAt(now)
                    .build());
        }
    }

    /**
     * retention 관련 요청 로직
     * @param userId
     * @param videoId
     */
    @Transactional
    public void syncRetention(long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = findVideoWithOwnership(userId, videoId);
        String googleId = getGoogleId(userId);

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                video.getCreatedAt().toLocalDate(),
                LocalDate.now().minusDays(3),
                RETENTION_METRICS,
                video.getYoutubeVideoId(),
                "elapsedVideoTimeRatio"
        );

        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);

        if (response.rows() == null || response.rows().size() != 100) {
            throw new ApiException(ErrorDefine.RETENTION_INVALID);
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());

        audienceRetentionRepository.deleteByVideoId(videoId);

        LocalDateTime now = LocalDateTime.now();
        List<AudienceRetention> retentions = response.rows().stream()
                .map(row -> AudienceRetention.builder()
                        .video(video)
                        .timeRatio(toDouble(row.get(indexMap.get("elapsedVideoTimeRatio"))))
                        .retentionRate(toDouble(row.get(indexMap.get("audienceWatchRatio"))))
                        .collectedAt(now)
                        .build())
                .toList();

        // relativeRetentionPerformance 평균 계산
        double relativeRetentionAvg = response.rows().stream()
                .mapToDouble(row -> toDouble(row.get(indexMap.get("relativeRetentionPerformance"))))
                .average()
                .orElse(0.0);

        videoAnalyticsRepository.findByVideoId(videoId).ifPresent(analytics ->
                analytics.updateRelativeRetention(relativeRetentionAvg)
        );

        audienceRetentionRepository.saveAll(retentions);
    }

    /**
     * unsubscribed 관련 로직, analytics에서 동작 안함, channel 기준으로만 unsubscribed가 가능한건지, video 단일 기준도 가능한건지 확인 필요..
     * @param userId
     * @param videoId
     */
    @Transactional
    public void syncUnsubscribedStats(long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = findVideoWithOwnership(userId, videoId);
        String googleId = getGoogleId(userId);

        Optional<VideoAnalytics> existing = videoAnalyticsRepository.findByVideoId(videoId);
        if (existing.isEmpty()) {
            return;
        }

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                video.getCreatedAt().toLocalDate(),
                LocalDate.now().minusDays(3),
                UNSUBSCRIBED_METRICS,
                video.getYoutubeVideoId(),
                "subscribedStatus"
        );

        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);

        log.info("columnHeaders: {}", response.columnHeaders());
        log.info("rows: {}", response.rows());

        if (response.rows() == null || response.rows().isEmpty()) {
            return;
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());

        List<Object> unsubscribedRow = response.rows().stream()
                .filter(row -> "UNSUBSCRIBED".equals(row.get(indexMap.get("subscribedStatus"))))
                .findFirst()
                .orElse(null);

        if (unsubscribedRow == null) {
            return;
        }

        Long unsubscribedViewCount = toLong(unsubscribedRow.get(indexMap.get("views")));

        existing.get().updateUnsubscribed(unsubscribedViewCount);
    }

    private Video findVideoWithOwnership(UUID userId, long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));
        if (!video.getChannel().getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
        return video;
    }

    private String getGoogleId(UUID userId) {
        User user = userReadRepository.getReferenceById(userId);
        return user.getProviderId();
    }

    private Map<String, Integer> buildIndexMap(List<YoutubeAnalyticsVideoResponse.ColumnHeader> headers) {
        return IntStream.range(0, headers.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> headers.get(i).name(),
                        i -> i
                ));
    }
}
