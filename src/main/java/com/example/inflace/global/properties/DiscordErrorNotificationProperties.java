package com.example.inflace.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord.error")
public record DiscordErrorNotificationProperties(
        String webhookUrl
) {
    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
