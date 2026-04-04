package com.example.inflace.domain.user.presentation;

import java.time.LocalDateTime;
import java.util.List;

public record UserChannelMainResponse(
        String profileImageUrl,
        String name,
        String youtubeStudioUrl,
        String channelHandle,
        List<String> category,
        LocalDateTime enteredAt,
        Long subscriberCount,
        Long videoCount,
        LocalDateTime latestUploadDate
) {
}
