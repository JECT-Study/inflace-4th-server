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
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final VideoTagRepository videoTagRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;

    @Transactional(readOnly=true)
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

    @Transactional(readOnly=true)
    public VideoStatsResponse getVideoStats(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.ANALYTICS_DATA_NOT_FOUND));

        VideoStatsChannelAverages channelAverages = videoStatsRepository.findChannelAverages(video.getChannel().getId());
        return VideoStatsResponse.from(videoStats, videoAnalytics, channelAverages);
    }

    @Transactional(readOnly=true)
    public AudienceRetentionResponse getRetention(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        List<AudienceRetention> retentionList = audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(videoId);
        if (retentionList.isEmpty()) {
            throw new ApiException(ErrorDefine.ANALYTICS_DATA_NOT_FOUND);
        }

        int durationSeconds = video.getDurationSeconds() != null ? video.getDurationSeconds() : 0;
        return AudienceRetentionResponse.from(retentionList, durationSeconds);
    }

    @Transactional(readOnly=true)
    public DropPointsResponse getDropPoints(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        Integer durationSeconds = video.getDurationSeconds();
        if (durationSeconds == null || durationSeconds == 0) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        List<AudienceRetention> retentionList = audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(videoId);
        if (retentionList.isEmpty()) {
            throw new ApiException(ErrorDefine.ANALYTICS_DATA_NOT_FOUND);
        }
        if (retentionList.size() != 100) {
            throw new ApiException(ErrorDefine.RETENTION_INVALID);
        }

        return DropPointsResponse.from(retentionList, durationSeconds);
    }

    @Transactional(readOnly=true)
    public RetentionSummaryResponse getRetentionSummary(Long videoId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.ANALYTICS_DATA_NOT_FOUND));

        return RetentionSummaryResponse.from(videoStats, videoAnalytics);
    }

    private void validateVideoOwnership(Video video, UUID userId) {
        if (!video.getChannel().getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }

    public Map<Long, VideoStats> getVideoStatsMap(List<Video> videos) {
        if (videos.isEmpty()) {
            return Map.of();
        }

        List<Long> videoIds = videos.stream()
                .map(Video::getId)
                .toList();

        List<VideoStats> videoStatsList = videoStatsRepository.findAllByVideoIdIn(videoIds);
        Map<Long, VideoStats> result = new HashMap<>();
        for (VideoStats videoStats : videoStatsList) {
            result.put(videoStats.getVideo().getId(), videoStats);
        }
        return result;
    }
}
