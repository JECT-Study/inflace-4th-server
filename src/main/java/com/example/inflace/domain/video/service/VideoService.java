package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.*;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;

    public VideoMetaResponse getVideoMeta(Long videoId) {
        // 영상 목록에서 클릭 후 이동, 외부 API 필요하지 않음
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        return VideoMetaResponse.from(video);
    }

    public VideoStatsResponse getVideoStats(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        VideoStats videoStats = videoStatsRepository.findByVideo(video)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_STATS_NOT_FOUND));

        return VideoStatsResponse.from(videoStats, 0L, 0L);
    }
}
