package com.example.inflace.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "channel.sync")
public record ChannelSyncProperties(
        long cooldownMillis
) {
}
