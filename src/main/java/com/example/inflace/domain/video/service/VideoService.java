package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.*;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;

    public VideoMetaResponse getVideoMeta(long userId, Long videoId) {
        // 영상 목록에서 클릭 후 이동, 외부 API 필요하지 않음
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        return VideoMetaResponse.from(video);
    }

    public VideoStatsResponse getVideoStats(long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideo(video)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));

        return VideoStatsResponse.from(videoStats, 0L, 0L);
    }

    public AudienceRetentionResponse getRetention(long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        // 소유자 확인
        validateVideoOwnership(video, userId);

        List<AudienceRetention> retentionList = audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(videoId);
        if (retentionList.isEmpty()) {
            throw new ApiException(ErrorDefine.RETENTION_NOT_FOUND);
        }

        double duration = video.getDuration() != null ? video.getDuration() : 0.0;
        return AudienceRetentionResponse.from(retentionList, duration);
    }

    public DropPointsResponse getDropPoints(long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        Double duration = video.getDuration();
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

    public RetentionSummaryResponse getRetentionSummary(long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        validateVideoOwnership(video, userId);

        VideoStats videoStats = videoStatsRepository.findByVideo(video)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));

        return RetentionSummaryResponse.from(videoStats);
    }

    private void validateVideoOwnership(Video video, long userId) {
        if (video.getChannel().getUser().getId() != userId) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }
}
