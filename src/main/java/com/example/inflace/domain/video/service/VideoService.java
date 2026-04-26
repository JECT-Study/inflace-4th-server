package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.domain.VideoTag;
import com.example.inflace.domain.video.dto.*;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.domain.video.repository.VideoTagRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final VideoTagRepository videoTagRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;

    @ReadOnlyTransactional
    public VideoMetaResponse getVideoMeta(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        // 영상 목록에서 클릭 후 이동, 외부 API 필요하지 않음
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        List<String> hashtags = videoTagRepository.findAllByVideoId(videoId).stream()
                .map(VideoTag::getTag)
                .toList();

        return VideoMetaResponse.from(video, hashtags);
    }

    @ReadOnlyTransactional
    public VideoStatsResponse getVideoStats(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId).orElse(null);

        return VideoStatsResponse.from(videoStats, videoAnalytics, 0L, 0L);
    }

    @ReadOnlyTransactional
    public AudienceRetentionResponse getRetention(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        List<AudienceRetention> retentionList = audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(videoId);
        if (retentionList.isEmpty()) {
            throw new ApiException(ErrorDefine.RETENTION_NOT_FOUND);
        }

        double duration = video.getDurationSeconds() != null ? video.getDurationSeconds().doubleValue() : 0.0;
        return AudienceRetentionResponse.from(retentionList, duration);
    }

    @ReadOnlyTransactional
    public DropPointsResponse getDropPoints(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        Double duration = video.getDurationSeconds() != null ? video.getDurationSeconds().doubleValue() : null;
        if (duration == null || duration == 0) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        List<AudienceRetention> retentionList = audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(videoId);
        if (retentionList.isEmpty()) {
            throw new ApiException(ErrorDefine.RETENTION_NOT_FOUND);
        }
        if (retentionList.size() != 100) {
            throw new ApiException(ErrorDefine.RETENTION_INVALID);
        }

        return DropPointsResponse.from(retentionList, duration);
    }

    @ReadOnlyTransactional
    public RetentionSummaryResponse getRetentionSummary(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId).orElse(null);

        return RetentionSummaryResponse.from(videoStats, videoAnalytics);
    }

    private void validateVideoOwnership(Video video, UUID userId) {
        if (!video.getChannel().getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }
}
