package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.dto.VideoMetaResponse;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final VideoRepository videoRepository;

    public VideoMetaResponse getVideoMeta(Long videoId) {
        // 영상 목록에서 클릭 후 이동, 외부 API 필요하지 않음
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

        return VideoMetaResponse.from(video);
    }

    private YoutubeDataVideoResponse getYoutubeDataVideo(String videoId, String parts) {
        return youtubeDataApiClient.getYoutubeVideo(videoId, parts);
    }

    private YoutubeAnalyticsVideoResponse getYoutubeAnalyticsVideo(String googleId,
                                                                   YoutubeAnalyticsVideoRequest request) {
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        return response;
    }
}
