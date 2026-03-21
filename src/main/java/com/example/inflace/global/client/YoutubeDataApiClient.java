package com.example.inflace.global.client;

import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.global.properties.YoutubeProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class YoutubeDataApiClient {

    private static final String CHANNELS_PATH = "/channels";

    private final RestClient restClient;
    private final YoutubeProperties youtubeProperties;

    public YoutubeDataChannelResponse getYoutubeChannels(String channelId, String parts) {
        URI uri = UriComponentsBuilder
                .fromUriString(youtubeProperties.dataApi().baseUrl())
                .path(CHANNELS_PATH)
                .queryParam("part", parts)
                .queryParam("id", channelId)
                .queryParam("key", youtubeProperties.dataApi().apiKey())
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(YoutubeDataChannelResponse.class);
    }
}
