package com.example.inflace.global.client;

import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.properties.YoutubeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class YoutubeAnalyticsApiClient {

    private static final String ANALYTICS_PATH = "/reports";
    private static final String CHANNEL_IDS = "channel==MINE";

    private final RestClient restClient;
    private final YoutubeProperties youtubeProperties;

    public YoutubeAnalyticsVideoResponse getYoutubeAnalytics(YoutubeAnalyticsVideoRequest request) {
        URI uri = UriComponentsBuilder
                .fromUriString(youtubeProperties.analyticsApi().baseUrl())
                .path(ANALYTICS_PATH)
                .queryParam("startDate", request.startDate().toString())
                .queryParam("endDate", request.endDate().toString())
                .queryParam("ids", CHANNEL_IDS)
                .queryParam("metrics", request.formattedMetricsList())
                .build()
                .toUri();

        // TODO : 차후 header에 access token 실어 보내는 방식으로 변경 예정
        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + youtubeProperties.analyticsApi().accessToken())
                .retrieve()
                .body(YoutubeAnalyticsVideoResponse.class);
    }
}
