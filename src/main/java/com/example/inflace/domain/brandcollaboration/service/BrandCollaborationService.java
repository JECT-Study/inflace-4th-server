package com.example.inflace.domain.brandcollaboration.service;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.client.YoutubeSearchApiClient;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.global.client.YoutubeSearchApiClient.YoutubeSearchListResponse;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.global.response.CustomSort;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandCollaborationService {

    private static final String VIDEO_PARTS = "snippet,statistics,contentDetails";
    private static final int SHORTS_MAX_DURATION_SECONDS = 180;
    private static final String CHANNEL_PARTS = "snippet";

    private final YoutubeSearchApiClient youtubeSearchApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;

    public CursorSliceResponse<BrandCollaborationVideoResponse> search(BrandCollaborationSearchCondition condition) {
        validateKeywords(condition.includeKeywords(), condition.excludeKeywords());

        String q = buildQuery(condition.brandName(), condition.includeKeywords(), condition.excludeKeywords());
        String youtubePageToken = decodePageToken(condition.cursor(), condition.sortCriteriaValue(), condition.sortOrder().name());

        YoutubeSearchListResponse searchResponse = youtubeSearchApiClient.search(
                q,
                youtubePageToken,
                condition.youtubeOrder(),
                null,
                condition.categoryId(),
                condition.regionCode(),
                condition.languageCode(),
                condition.pageSize(),
                condition.startDate(),
                condition.endDate()
        );

        if (searchResponse == null || searchResponse.items() == null || searchResponse.items().isEmpty()) {
            return emptyResponse(condition);
        }

        List<String> videoIds = searchResponse.items().stream()
                .map(item -> item.id().videoId())
                .filter(StringUtils::hasText)
                .toList();

        List<YoutubeDataVideoResponse.Item> videoItems = youtubeDataApiClient.getYoutubeVideos(videoIds, VIDEO_PARTS);
        List<YoutubeDataVideoResponse.Item> filtered = applyStatisticsFilter(videoItems, condition);

        if (filtered.isEmpty()) {
            return emptyResponse(condition);
        }

        Map<String, YoutubeDataChannelResponse.Item> channelMap = fetchChannelMap(filtered);
        List<BrandCollaborationVideoResponse> content = buildContent(filtered, channelMap);

        if (condition.sortOrder().name().equals("ASC")) {
            Collections.reverse(content);
        }

        String nextCursor = encodeNextCursor(
                searchResponse.nextPageToken(),
                condition.sortCriteriaValue(),
                condition.sortOrder().name()
        );

        return new CursorSliceResponse<>(
                content,
                new CursorSliceResponse.PageInfo(condition.pageSize(), content.size(), nextCursor, nextCursor != null),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    private void validateKeywords(List<String> includeKeywords, List<String> excludeKeywords) {
        boolean hasDuplicate = includeKeywords.stream().anyMatch(excludeKeywords::contains);
        if (hasDuplicate) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    private String buildQuery(String brandName, List<String> includeKeywords, List<String> excludeKeywords) {
        StringBuilder q = new StringBuilder(brandName);

        if (!includeKeywords.isEmpty()) {
            q.append(" ").append(String.join("|", includeKeywords));
        }
        for (String keyword : excludeKeywords) {
            q.append(" -").append(keyword);
        }

        return q.toString();
    }

    private String decodePageToken(String cursor, String expectedSortCriteria, String expectedSortOrder) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] tokens = decoded.split("\\|", 3);

            if (tokens.length != 3) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }
            if (!tokens[0].equals(expectedSortCriteria) || !tokens[1].equals(expectedSortOrder)) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }
            return tokens[2];
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    private String encodeNextCursor(String youtubePageToken, String sortCriteria, String sortOrder) {
        if (!StringUtils.hasText(youtubePageToken)) {
            return null;
        }
        String raw = sortCriteria + "|" + sortOrder + "|" + youtubePageToken;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private List<YoutubeDataVideoResponse.Item> applyStatisticsFilter(
            List<YoutubeDataVideoResponse.Item> items,
            BrandCollaborationSearchCondition condition
    ) {
        return items.stream()
                .filter(item -> item.statistics() != null)
                .filter(item -> parseLong(item.statistics().viewCount()) >= condition.minViews())
                .filter(item -> parseLong(item.statistics().likeCount()) >= condition.minLikes())
                .filter(item -> parseLong(item.statistics().commentCount()) >= condition.minComments())
                .filter(item -> matchesVideoFormat(item, condition.videoFormat()))
                .toList();
    }

    private boolean matchesVideoFormat(YoutubeDataVideoResponse.Item item, String videoFormat) {
        ChannelVideoFormat format = ChannelVideoFormat.ALL;
        if (StringUtils.hasText(videoFormat)) {
            try {
                format = ChannelVideoFormat.valueOf(videoFormat.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        if (format == ChannelVideoFormat.ALL) {
            return true;
        }
        if (item.contentDetails() == null || !StringUtils.hasText(item.contentDetails().duration())) {
            return true;
        }
        int durationSeconds = (int) AnalyticsCalculator.parseIso8601Duration(item.contentDetails().duration());
        boolean isShort = durationSeconds <= SHORTS_MAX_DURATION_SECONDS;
        return format == ChannelVideoFormat.SHORT_FORM ? isShort : !isShort;
    }

    private Map<String, YoutubeDataChannelResponse.Item> fetchChannelMap(List<YoutubeDataVideoResponse.Item> items) {
        List<String> channelIds = items.stream()
                .map(item -> item.snippet() != null ? item.snippet().channelId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (channelIds.isEmpty()) {
            return Map.of();
        }

        YoutubeDataChannelResponse channelResponse = youtubeDataApiClient.getYoutubeChannels(
                String.join(",", channelIds), CHANNEL_PARTS);

        if (channelResponse == null || channelResponse.items() == null) {
            return Map.of();
        }

        return channelResponse.items().stream()
                .collect(Collectors.toMap(YoutubeDataChannelResponse.Item::id, c -> c));
    }

    private List<BrandCollaborationVideoResponse> buildContent(
            List<YoutubeDataVideoResponse.Item> videoItems,
            Map<String, YoutubeDataChannelResponse.Item> channelMap
    ) {
        List<BrandCollaborationVideoResponse> result = new ArrayList<>();
        for (YoutubeDataVideoResponse.Item video : videoItems) {
            String channelId = video.snippet() != null ? video.snippet().channelId() : null;
            YoutubeDataChannelResponse.Item channel = channelId != null ? channelMap.get(channelId) : null;

            String channelThumbnailUrl = null;
            if (channel != null && channel.snippet() != null && channel.snippet().thumbnails() != null
                    && channel.snippet().thumbnails().defaultThumbnail() != null) {
                channelThumbnailUrl = channel.snippet().thumbnails().defaultThumbnail().url();
            }

            result.add(new BrandCollaborationVideoResponse(
                    video.id(),
                    video.snippet() != null ? video.snippet().title() : null,
                    video.snippet() != null && video.snippet().thumbnails() != null
                            && video.snippet().thumbnails().high() != null
                            ? video.snippet().thumbnails().high().url() : null,
                    video.snippet() != null ? video.snippet().publishedAt() : null,
                    parseLong(video.statistics() != null ? video.statistics().viewCount() : null),
                    parseLong(video.statistics() != null ? video.statistics().likeCount() : null),
                    parseLong(video.statistics() != null ? video.statistics().commentCount() : null),
                    channelId,
                    channel != null && channel.snippet() != null ? channel.snippet().title() : null,
                    channelThumbnailUrl
            ));
        }
        return result;
    }

    private CursorSliceResponse<BrandCollaborationVideoResponse> emptyResponse(BrandCollaborationSearchCondition condition) {
        return new CursorSliceResponse<>(
                List.of(),
                new CursorSliceResponse.PageInfo(condition.pageSize(), 0, null, false),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
