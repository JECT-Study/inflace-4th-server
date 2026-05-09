package com.example.inflace.domain.brandcollaboration.service;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationTrendsRequest;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationTrendsResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.client.YoutubeSearchApiClient;
import com.example.inflace.global.client.YoutubeSearchApiClient.YoutubeSearchListResponse;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.global.response.CustomSort;
import com.example.inflace.global.util.AnalyticsCalculator;
import com.example.inflace.infra.openai.OpenAiModel;
import com.example.inflace.infra.openai.OpenAiSendRequest;
import com.example.inflace.infra.openai.prompt.BrandCollaborationTrendsPrompt;
import com.example.inflace.infra.openai.service.OpenAiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrandCollaborationService {

    private static final String VIDEO_PARTS = "snippet,statistics,contentDetails";
    // https://developers.google.com/youtube/v3/docs/videos/list
    private static final String TRENDS_VIDEO_PARTS = "snippet,statistics";
    private static final int SHORTS_MAX_DURATION_SECONDS = 180;
    private static final String CHANNEL_PARTS = "snippet";
    // https://developers.google.com/youtube/v3/docs/channels/list
    private static final String CHANNEL_PARTS_WITH_STATS = "snippet,statistics";

    private final YoutubeSearchApiClient youtubeSearchApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

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

        Map<String, YoutubeDataChannelResponse.Item> channelMap = fetchChannelMap(filtered, CHANNEL_PARTS);
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

    public BrandCollaborationTrendsResponse analyzeTrends(BrandCollaborationTrendsRequest request) {
        List<YoutubeDataVideoResponse.Item> videoItems = youtubeDataApiClient.getYoutubeVideos(
                request.youtubeVideoIds(), TRENDS_VIDEO_PARTS);

        if (videoItems.isEmpty()) {
            return new BrandCollaborationTrendsResponse(List.of(), null);
        }

        Map<String, YoutubeDataChannelResponse.Item> channelMap = fetchChannelMap(videoItems, CHANNEL_PARTS_WITH_STATS);

        try {
            String raw = openAiService.sendChatMessage(new OpenAiSendRequest(
                    BrandCollaborationTrendsPrompt.systemMessage(),
                    BrandCollaborationTrendsPrompt.humanMessage(videoItems, channelMap),
                    null,
                    OpenAiModel.GPT_4O
            ));
            return objectMapper.readValue(raw, BrandCollaborationTrendsResponse.class);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Failed to analyze brand collaboration trends. videoCount={}", videoItems.size(), e);
            return new BrandCollaborationTrendsResponse(List.of(), null);
        }
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
                .filter(item -> matchesVideoFormat(item, condition.videoFormatEnum()))
                .toList();
    }

    private boolean matchesVideoFormat(YoutubeDataVideoResponse.Item item, ChannelVideoFormat format) {
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

    private Map<String, YoutubeDataChannelResponse.Item> fetchChannelMap(
            List<YoutubeDataVideoResponse.Item> items, String parts
    ) {
        List<String> channelIds = items.stream()
                .map(item -> item.snippet() != null ? item.snippet().channelId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (channelIds.isEmpty()) {
            return Map.of();
        }

        YoutubeDataChannelResponse channelResponse = youtubeDataApiClient.getYoutubeChannels(
                String.join(",", channelIds), parts);

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
