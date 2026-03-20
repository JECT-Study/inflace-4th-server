package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.global.client.YoutubeDataApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeDataApiClient youtubeDataApiClient;

    public YoutubeDataChannelResponse getYoutubeChannel(String channelId) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId);
        return response;
    }
}
