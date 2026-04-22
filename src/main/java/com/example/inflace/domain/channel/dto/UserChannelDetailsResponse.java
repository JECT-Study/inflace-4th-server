package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.channel.domain.Channel;

public record UserChannelDetailsResponse(
        String youtubeChannelId,
        String youtubeChannelName,
        String youtubeChannelProfileImageUrl
) {
    public static UserChannelDetailsResponse from(Channel channel) {
        return new UserChannelDetailsResponse(
                channel.getYoutubeChannelId(),
                channel.getName(),
                null
        );
    }
}
