package com.example.inflace.domain.channel.dto;

import java.util.UUID;

public record RefreshChannelTarget(
        UUID userId,
        String googleId,
        Long channelId,
        String youtubeChannelId
) {
}
