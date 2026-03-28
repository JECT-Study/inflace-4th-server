package com.example.inflace.global.client;

import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
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
    private final GoogleAccessTokenStore googleAccessTokenStore;

    public YoutubeAnalyticsVideoResponse getYoutubeAnalytics(String googleId, YoutubeAnalyticsVideoRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(youtubeProperties.analyticsApi().baseUrl())
                .path(ANALYTICS_PATH)
                .queryParam("startDate", request.startDate().toString())
                .queryParam("endDate", request.endDate().toString())
                .queryParam("ids", CHANNEL_IDS)
                .queryParam("metrics", request.formattedMetricsList());

        // video filter 없이 가져오는 경우가 있어서, 분리
        if (request.youtubeVideoId() != null) {
            builder.queryParam("filters", "video==" + request.youtubeVideoId());
        }

        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessTokenStore.getAccessToken(googleId))
                .retrieve()
                .body(YoutubeAnalyticsVideoResponse.class);
    }
}
