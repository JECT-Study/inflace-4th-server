package com.example.inflace.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube")
public record YoutubeProperties(
        DataApi dataApi
) {
    public record DataApi(
            String baseUrl,
            String apiKey
    ){}
}
