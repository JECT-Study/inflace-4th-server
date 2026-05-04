package com.example.inflace.global.client;

import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.global.properties.YoutubeProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class YoutubeDataApiClient {

    private static final int YOUTUBE_MAX_RESULTS = 50;
    private static final String CHANNELS_PATH = "/channels";
    private static final String VIDEOS_PATH = "/videos";
    private static final String PLAYLIST_ITEMS_PATH = "/playlistItems";

    private final RestClient restClient;
    private final YoutubeProperties youtubeProperties;
    private final GoogleAccessTokenStore googleAccessTokenStore;

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

    public YoutubeDataChannelResponse getMyChannel(String googleId, String parts) {
        URI uri = UriComponentsBuilder
                .fromUriString(youtubeProperties.dataApi().baseUrl())
                .path(CHANNELS_PATH)
                .queryParam("part", parts)
                .queryParam("mine", true)
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessTokenStore.getAccessToken(googleId))
                .retrieve()
                .body(YoutubeDataChannelResponse.class);
    }

    public YoutubeDataVideoResponse getYoutubeVideo(String videoId, String parts) {
        URI uri = UriComponentsBuilder
                .fromUriString(youtubeProperties.dataApi().baseUrl())
                .path(VIDEOS_PATH)
                .queryParam("part", parts)
                .queryParam("id", videoId)
                .queryParam("key", youtubeProperties.dataApi().apiKey())
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(YoutubeDataVideoResponse.class);
    }

    public List<String> getMyVideoIds(String googleId, String uploadsPlaylistId) {
        if (!StringUtils.hasText(uploadsPlaylistId)) {
            return List.of();
        }

        Set<String> videoIds = new LinkedHashSet<>();
        String pageToken = null;

        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(youtubeProperties.dataApi().baseUrl())
                    .path(PLAYLIST_ITEMS_PATH)
                    .queryParam("part", "contentDetails")
                    .queryParam("playlistId", uploadsPlaylistId)
                    .queryParam("maxResults", YOUTUBE_MAX_RESULTS);

            if (StringUtils.hasText(pageToken)) {
                builder.queryParam("pageToken", pageToken);
            }

            PlaylistItemsResponse response = restClient.get()
                    .uri(builder.build().toUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessTokenStore.getAccessToken(googleId))
                    .retrieve()
                    .body(PlaylistItemsResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                break;
            }

            for (PlaylistItem item : response.items()) {
                if (item != null
                        && item.contentDetails() != null
                        && StringUtils.hasText(item.contentDetails().videoId())) {
                    videoIds.add(item.contentDetails().videoId());
                }
            }

            pageToken = response.nextPageToken();
            if (!StringUtils.hasText(pageToken)) {
                break;
            }
        }

        return new ArrayList<>(videoIds);
    }

    public List<YoutubeDataVideoResponse.Item> getYoutubeVideos(List<String> videoIds, String parts) {
        List<String> normalizedVideoIds = normalizeIds(videoIds);
        if (normalizedVideoIds.isEmpty()) {
            return List.of();
        }

        List<YoutubeDataVideoResponse.Item> items = new ArrayList<>();
        for (List<String> chunk : chunked(normalizedVideoIds, YOUTUBE_MAX_RESULTS)) {
            URI uri = UriComponentsBuilder
                    .fromUriString(youtubeProperties.dataApi().baseUrl())
                    .path(VIDEOS_PATH)
                    .queryParam("part", parts)
                    .queryParam("id", String.join(",", chunk))
                    .queryParam("key", youtubeProperties.dataApi().apiKey())
                    .build()
                    .toUri();

            YoutubeDataVideoResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(YoutubeDataVideoResponse.class);

            if (response != null && response.items() != null) {
                items.addAll(response.items());
            }
        }

        return items;
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private <T> List<List<T>> chunked(List<T> values, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            chunks.add(values.subList(start, Math.min(values.size(), start + size)));
        }
        return chunks;
    }

    public record PlaylistItemsResponse(
            String nextPageToken,
            List<PlaylistItem> items
    ) {
    }

    public record PlaylistItem(
            PlaylistContentDetails contentDetails
    ) {
    }

    public record PlaylistContentDetails(
            String videoId
    ) {
    }
}
