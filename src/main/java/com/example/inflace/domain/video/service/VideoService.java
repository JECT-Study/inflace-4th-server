package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;

    private YoutubeAnalyticsVideoResponse getYoutubeAnalyticsVideo(YoutubeAnalyticsVideoRequest request) {
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(request);
        return response;
    }
}
