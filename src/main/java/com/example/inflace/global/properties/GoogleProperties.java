package com.example.inflace.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
public record GoogleProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}
