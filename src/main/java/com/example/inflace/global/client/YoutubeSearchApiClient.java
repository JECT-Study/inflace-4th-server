package com.example.inflace.global.client;

import com.example.inflace.global.properties.YoutubeProperties;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class YoutubeSearchApiClient {

    private static final String SEARCH_PATH = "/search";

    private final RestClient restClient;
    private final YoutubeProperties youtubeProperties;

    // https://developers.google.com/youtube/v3/docs/search/list
    public YoutubeSearchListResponse search(
            String q,
            String pageToken,
            String order,
            String videoDuration,
            String categoryId,
            String regionCode,
            String relevanceLanguage,
            int maxResults,
            String publishedAfter,
            String publishedBefore
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(youtubeProperties.dataApi().baseUrl())
                .path(SEARCH_PATH)
                .queryParam("part", "id")
                .queryParam("type", "video")
                .queryParam("videoPaidProductPlacement", "true")
                .queryParam("q", q)
                .queryParam("order", order)
                .queryParam("maxResults", maxResults)
                .queryParam("key", youtubeProperties.dataApi().apiKey());

        if (StringUtils.hasText(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }
        if (StringUtils.hasText(publishedAfter)) {
            builder.queryParam("publishedAfter", publishedAfter);
        }
        if (StringUtils.hasText(publishedBefore)) {
            builder.queryParam("publishedBefore", publishedBefore);
        }
        if (StringUtils.hasText(videoDuration) && !"any".equals(videoDuration)) {
            builder.queryParam("videoDuration", videoDuration);
        }
        if (StringUtils.hasText(categoryId)) {
            builder.queryParam("videoCategoryId", categoryId);
        }
        if (StringUtils.hasText(regionCode)) {
            builder.queryParam("regionCode", regionCode);
        }
        if (StringUtils.hasText(relevanceLanguage)) {
            builder.queryParam("relevanceLanguage", relevanceLanguage);
        }

        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(YoutubeSearchListResponse.class);
    }

    public record YoutubeSearchListResponse(
            String nextPageToken,
            List<Item> items
    ) {
        public record Item(Id id) {}
        public record Id(String videoId) {}
    }
}
