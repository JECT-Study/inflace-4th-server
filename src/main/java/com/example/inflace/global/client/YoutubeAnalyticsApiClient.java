package com.example.inflace.global.client;

import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.properties.YoutubeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
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

        if (request.dimensions() != null) {
            builder.queryParam("dimensions", request.dimensions());
        }

        if (request.youtubeVideoId() != null) {
            String filterValue = "video==" + request.youtubeVideoId();

            // 시청 유지율(elapsedVideoTimeRatio) 조회 시 필수 필터 추가
            if ("elapsedVideoTimeRatio".equals(request.dimensions())) {
                filterValue += ";audienceType==ORGANIC";
            }

            builder.queryParam("filters", filterValue);
        }

        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessTokenStore.getAccessToken(googleId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.error("YouTube API 4xx error: {}", new String(res.getBody().readAllBytes()));
                    throw new ApiException(ErrorDefine.YOUTUBE_API_ERROR);  // 오류 추적이 어려워서 외부 api 통신 에러 로그 추가
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    log.error("YouTube API 5xx error: {}", new String(res.getBody().readAllBytes()));
                    throw new ApiException(ErrorDefine.YOUTUBE_API_ERROR);
                })
                .body(YoutubeAnalyticsVideoResponse.class);
    }
}
