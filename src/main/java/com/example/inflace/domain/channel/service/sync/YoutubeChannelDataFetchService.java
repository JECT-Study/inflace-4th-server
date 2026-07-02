package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse.Item;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class YoutubeChannelDataFetchService {

    private static final String CHANNEL_PARTS = "snippet,statistics,contentDetails";
    private static final String VIDEO_PARTS = "snippet,contentDetails,statistics,paidProductPlacementDetails";

    private final YoutubeDataApiClient youtubeDataApiClient;

    public YoutubeDataChannelResponse.Item fetchMyChannel(String googleId) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getMyChannel(googleId, CHANNEL_PARTS);
        return extractChannel(response);
    }

    public YoutubeDataChannelResponse.Item fetchChannel(String youtubeChannelId) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(youtubeChannelId, CHANNEL_PARTS);
        return extractChannel(response);
    }

    public List<Item> fetchVideos(String googleId, String uploadsPlaylistId) {
        if (!StringUtils.hasText(uploadsPlaylistId)) {
            return List.of();
        }

        List<String> videoIds = youtubeDataApiClient.getMyVideoIds(googleId, uploadsPlaylistId);
        List<YoutubeDataVideoResponse.Item> videoItems = youtubeDataApiClient.getYoutubeVideos(videoIds, VIDEO_PARTS);

        return videoItems.stream()
                .filter(item -> item != null && StringUtils.hasText(item.id()))
                .toList();
    }

    private YoutubeDataChannelResponse.Item extractChannel(YoutubeDataChannelResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            throw new ApiException(ErrorDefine.CHANNEL_NOT_FOUND);
        }
        return response.items().get(0);
    }
}
