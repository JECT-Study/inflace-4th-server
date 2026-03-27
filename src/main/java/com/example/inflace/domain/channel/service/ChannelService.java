package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.global.client.YoutubeDataApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeDataApiClient youtubeDataApiClient;

    private YoutubeDataChannelResponse getYoutubeChannel(String channelId, String parts) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId, parts);
        return response;
    }
}
