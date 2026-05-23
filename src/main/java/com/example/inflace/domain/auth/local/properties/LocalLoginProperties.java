package com.example.inflace.domain.auth.local.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-login")
public record LocalLoginProperties(
        String googleRedirectUri,
        String youtubeRedirectUri
) {
}
