package com.example.inflace.domain.channel.dto;

import java.time.LocalDateTime;

public record ChannelSyncResponse(
        Long channelId,
        String youtubeChannelId,
        LocalDateTime updatedAt
) {
}
