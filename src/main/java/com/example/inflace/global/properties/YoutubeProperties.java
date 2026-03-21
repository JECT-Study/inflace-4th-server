package com.example.inflace.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube")
public record YoutubeProperties(
        DataApi dataApi,
        AnalyticsApi analyticsApi
) {
    public record DataApi(
            String baseUrl,
            String apiKey
    ){}

    public record AnalyticsApi(
            String baseUrl,
            String accessToken  // TODO : 임시, 차후 로그인 구현 시 OAuth2AuthorizedClientService에서 꺼내서 헤더에 주입
    ){}
}
