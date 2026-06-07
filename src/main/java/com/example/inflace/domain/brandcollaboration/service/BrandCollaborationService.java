package com.example.inflace.domain.brandcollaboration.service;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationTrendsRequest;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationTrendsResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.youtubecategory.repository.YoutubeCategoryRepository;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
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
    private static final String CHANNEL_PARTS_WITH_STATS = "snippet,statistics,contentDetails";
    private static final int RECENT_UPLOADS_COUNT = 10;

    private final YoutubeSearchApiClient youtubeSearchApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final YoutubeCategoryRepository youtubeCategoryRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    public CursorSliceResponse<BrandCollaborationVideoResponse> search(BrandCollaborationSearchCondition condition) {
        validateKeywords(condition.includeKeywords(), condition.excludeKeywords());

        String q = buildQuery(condition.includeKeywords(), condition.excludeKeywords());
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
                condition.endDate(),
                null
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
            String nextCursor = encodeNextCursor(
                    searchResponse.nextPageToken(),
                    condition.sortCriteriaValue(),
                    condition.sortOrder().name()
            );
            return new CursorSliceResponse<>(
                    List.of(),
                    new CursorSliceResponse.PageInfo(condition.pageSize(), 0, nextCursor, nextCursor != null),
                    CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
            );
        }

        Map<String, YoutubeDataChannelResponse.Item> channelMap = fetchChannelMap(filtered, CHANNEL_PARTS);
        List<BrandCollaborationVideoResponse> content = buildContent(filtered, channelMap);

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

    private record TrendsAiResponse(
            List<String> commonKeywords,
            String keywordSummary,
            BrandCollaborationTrendsResponse.StrategyInsight strategyInsight
    ) {
    }

    public BrandCollaborationTrendsResponse analyzeTrends(BrandCollaborationTrendsRequest request) {
        List<YoutubeDataVideoResponse.Item> videoItems;
        Map<String, YoutubeDataChannelResponse.Item> channelMap;
        long avgSubscribers, minSubscribers, maxSubscribers;
        Double uploadIntervalDays;
        List<BrandCollaborationTrendsResponse.CategoryShare> categoryDistribution;

        try {
            videoItems = youtubeDataApiClient.getYoutubeVideos(request.youtubeVideoIds(), TRENDS_VIDEO_PARTS);

            if (videoItems.isEmpty()) {
                return new BrandCollaborationTrendsResponse(
                        new BrandCollaborationTrendsResponse.ContentKeywords(List.of(), null),
                        null,
                        null
                );
            }

            // channels.list (snippet,statistics,contentDetails) — 구독자 통계 + 업로드 플레이리스트 ID 확보
            channelMap = fetchChannelMap(videoItems, CHANNEL_PARTS_WITH_STATS);

            // 구독자 수: channels.list statistics.subscriberCount 직접 계산
            List<Long> subCounts = channelMap.values().stream()
                    .filter(c -> c.statistics() != null)
                    .map(c -> parseLong(c.statistics().subscriberCount()))
                    .filter(count -> count > 0)
                    .sorted()
                    .toList();
            avgSubscribers = subCounts.isEmpty() ? 0L : (long) subCounts.stream().mapToLong(Long::longValue).average().orElse(0);
            minSubscribers = subCounts.isEmpty() ? 0L : subCounts.getFirst();
            maxSubscribers = subCounts.isEmpty() ? 0L : subCounts.getLast();

            // 업로드 간격: 채널별 uploads 플레이리스트 최근 10개 publishedAt → 평균 간격(일)
            uploadIntervalDays = computeAvgUploadDays(channelMap);

            // 카테고리 분포: 영상 snippet.categoryId → YoutubeCategory 테이블 룩업 후 비율 계산
            categoryDistribution = computeCategoryDistribution(videoItems);
        } catch (RuntimeException e) {
            log.warn("Failed to fetch YouTube data for trends analysis. videoCount={}", request.youtubeVideoIds().size(), e);
            return new BrandCollaborationTrendsResponse(
                    new BrandCollaborationTrendsResponse.ContentKeywords(List.of(), null),
                    null,
                    null
            );
        }

        try {
            // AI: commonKeywords, keywordSummary, strategyInsight
            String raw = openAiService.sendChatMessage(new OpenAiSendRequest(
                    BrandCollaborationTrendsPrompt.systemMessage(),
                    BrandCollaborationTrendsPrompt.humanMessage(videoItems, channelMap),
                    null,
                    OpenAiModel.GPT_4O
            ));
            TrendsAiResponse ai = objectMapper.readValue(stripMarkdown(raw), TrendsAiResponse.class);
            return new BrandCollaborationTrendsResponse(
                    new BrandCollaborationTrendsResponse.ContentKeywords(ai.commonKeywords(), ai.keywordSummary()),
                    new BrandCollaborationTrendsResponse.ChannelCharacteristics(
                            channelMap.size(), avgSubscribers, minSubscribers, maxSubscribers,
                            uploadIntervalDays, categoryDistribution),
                    ai.strategyInsight()
            );
        } catch (JsonProcessingException | RuntimeException e) {
            // AI 실패 시 백엔드 계산값만 반환, AI 필드는 null
            log.warn("Failed to analyze brand collaboration trends. videoCount={}", videoItems.size(), e);
            return new BrandCollaborationTrendsResponse(
                    new BrandCollaborationTrendsResponse.ContentKeywords(List.of(), null),
                    new BrandCollaborationTrendsResponse.ChannelCharacteristics(
                            channelMap.size(), avgSubscribers, minSubscribers, maxSubscribers,
                            uploadIntervalDays, categoryDistribution),
                    null
            );
        }
    }

    private List<BrandCollaborationTrendsResponse.CategoryShare> computeCategoryDistribution(
            List<YoutubeDataVideoResponse.Item> videoItems
    ) {
        List<Integer> categoryIds = videoItems.stream()
                .map(item -> item.snippet() != null ? item.snippet().categoryId() : null)
                .filter(StringUtils::hasText)
                .map(id -> {
                    try { return Integer.parseInt(id); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();

        if (categoryIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> titleMap = youtubeCategoryRepository
                .findByYoutubeCategoryIdIn(categoryIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(c -> c.getYoutubeCategoryId(), c -> c.getTitle()));

        int total = categoryIds.size();
        return categoryIds.stream()
                .collect(Collectors.groupingBy(
                        id -> titleMap.getOrDefault(id, "기타"),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new BrandCollaborationTrendsResponse.CategoryShare(
                        e.getKey(),
                        (int) Math.round((double) e.getValue() / total * 100)
                ))
                .sorted(Comparator.comparingInt(BrandCollaborationTrendsResponse.CategoryShare::percentage).reversed())
                .toList();
    }

    private Double computeAvgUploadDays(Map<String, YoutubeDataChannelResponse.Item> channelMap) {
        OptionalDouble avg = channelMap.values().stream()
                .filter(c -> c.contentDetails() != null && c.contentDetails().relatedPlaylists() != null)
                .map(c -> c.contentDetails().relatedPlaylists().uploads())
                .filter(StringUtils::hasText)
                .mapToDouble(playlistId -> {
                    List<Instant> instants = youtubeDataApiClient
                            .getRecentUploadDates(playlistId, RECENT_UPLOADS_COUNT)
                            .stream().map(Instant::parse).sorted().toList();
                    if (instants.size() < 2) return Double.NaN;
                    long spanSeconds = instants.getLast().getEpochSecond() - instants.getFirst().getEpochSecond();
                    return spanSeconds / 86400.0 / (instants.size() - 1);
                })
                .filter(d -> !Double.isNaN(d) && d > 0)
                .average();

        return avg.isPresent() ? avg.getAsDouble() : null;
    }

    private void validateKeywords(List<String> includeKeywords, List<String> excludeKeywords) {
        boolean hasDuplicate = includeKeywords.stream().anyMatch(excludeKeywords::contains);
        if (hasDuplicate) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    private String buildQuery(List<String> includeKeywords, List<String> excludeKeywords) {
        StringBuilder q = new StringBuilder(String.join("|", includeKeywords));

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

    private String stripMarkdown(String raw) {
        String stripped = raw.strip();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
        }
        return stripped;
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
