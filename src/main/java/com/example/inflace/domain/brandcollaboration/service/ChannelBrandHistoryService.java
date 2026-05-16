package com.example.inflace.domain.brandcollaboration.service;

import com.example.inflace.domain.brand.service.BrandService;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.service.insight.InfluencerInsightScoreCalculator;
import com.example.inflace.domain.youtubecategory.repository.YoutubeCategoryRepository;
import com.example.inflace.domain.brandcollaboration.dto.request.ChannelBrandHistorySearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryAnalysisResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryVideoResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.client.YoutubeSearchApiClient;
import com.example.inflace.global.client.YoutubeSearchApiClient.YoutubeSearchListResponse;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.global.response.CustomSort;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelBrandHistoryService {

    private static final String VIDEO_PARTS = "snippet,statistics,contentDetails";
    private static final String CHANNEL_STATS_PARTS = "statistics";
    private static final int SHORTS_MAX_DURATION_SECONDS = 180;
    private static final int ANALYSIS_MAX_VIDEOS = 50; // YouTube Search API maxResults 상한
    private static final String FORMAT_LONG_FORM = "LONG_FORM";
    private static final String FORMAT_SHORT_FORM = "SHORT_FORM";

    private final YoutubeSearchApiClient youtubeSearchApiClient;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final YoutubeCategoryRepository youtubeCategoryRepository;
    private final BrandService brandService;
    private final InfluencerInsightScoreCalculator scoreCalculator;

    public CursorSliceResponse<ChannelBrandHistoryVideoResponse> search(String channelId, ChannelBrandHistorySearchCondition condition) {
        String youtubePageToken = decodePageToken(condition.cursor(), condition.sortCriteriaValue());

        YoutubeSearchListResponse searchResponse = youtubeSearchApiClient.search(
                null,
                youtubePageToken,
                condition.youtubeOrder(),
                null,
                condition.categoryId(),
                null,
                null,
                condition.pageSize(),
                condition.startDate(),
                condition.endDate(),
                channelId
        );

        if (searchResponse == null || searchResponse.items() == null || searchResponse.items().isEmpty()) {
            return emptyResponse(condition);
        }

        List<String> videoIds = searchResponse.items().stream()
                .map(item -> item.id().videoId())
                .filter(StringUtils::hasText)
                .toList();

        List<YoutubeDataVideoResponse.Item> videoItems = youtubeDataApiClient.getYoutubeVideos(videoIds, VIDEO_PARTS);
        List<YoutubeDataVideoResponse.Item> filtered = applyVideoFormatFilter(videoItems, condition.videoFormatEnum());

        if (filtered.isEmpty()) {
            String nextCursor = encodeNextCursor(searchResponse.nextPageToken(), condition.sortCriteriaValue());
            return new CursorSliceResponse<>(
                    List.of(),
                    new CursorSliceResponse.PageInfo(condition.pageSize(), 0, nextCursor, nextCursor != null),
                    CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
            );
        }

        Map<Integer, String> categoryTitleMap = buildCategoryTitleMap(filtered);
        Map<String, String> aliasToNameMap = collectAliasToNameMap(filtered);
        List<ChannelBrandHistoryVideoResponse> content = buildContent(filtered, categoryTitleMap, aliasToNameMap);

        String nextCursor = encodeNextCursor(searchResponse.nextPageToken(), condition.sortCriteriaValue());
        return new CursorSliceResponse<>(
                content,
                new CursorSliceResponse.PageInfo(condition.pageSize(), content.size(), nextCursor, nextCursor != null),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    // https://developers.google.com/youtube/v3/docs/search/list
    public ChannelBrandHistoryAnalysisResponse analysis(String channelId, ChannelBrandHistorySearchCondition condition) {
        YoutubeSearchListResponse searchResponse = youtubeSearchApiClient.search(
                null,
                null,
                condition.youtubeOrder(),
                null,
                condition.categoryId(),
                null,
                null,
                ANALYSIS_MAX_VIDEOS,
                condition.startDate(),
                condition.endDate(),
                channelId
        );

        if (searchResponse == null || searchResponse.items() == null || searchResponse.items().isEmpty()) {
            return emptyAnalysis();
        }

        List<String> videoIds = searchResponse.items().stream()
                .map(item -> item.id().videoId())
                .filter(StringUtils::hasText)
                .toList();

        List<YoutubeDataVideoResponse.Item> videoItems = youtubeDataApiClient.getYoutubeVideos(videoIds, VIDEO_PARTS);
        List<YoutubeDataVideoResponse.Item> filtered = applyVideoFormatFilter(videoItems, condition.videoFormatEnum());

        if (filtered.isEmpty()) {
            return emptyAnalysis();
        }

        Map<Integer, String> categoryTitleMap = buildCategoryTitleMap(filtered);
        ChannelBrandHistoryAnalysisResponse.AdScore adScore;
        try {
            adScore = computeAdScore(channelId, filtered);
        } catch (Exception e) {
            log.warn("adScore 계산 실패: channelId={}, error={}", channelId, e.getMessage(), e);
            adScore = null;
        }

        return new ChannelBrandHistoryAnalysisResponse(
                filtered.size(),
                computeAvgViewsByContentType(filtered),
                computeCategoryDistribution(filtered, categoryTitleMap),
                computeContentTypeDistribution(filtered),
                adScore
        );
    }

    private List<YoutubeDataVideoResponse.Item> applyVideoFormatFilter(
            List<YoutubeDataVideoResponse.Item> items,
            ChannelVideoFormat format
    ) {
        if (format == ChannelVideoFormat.ALL) {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    if (item.contentDetails() == null || !StringUtils.hasText(item.contentDetails().duration())) {
                        return true;
                    }
                    int durationSeconds = (int) AnalyticsCalculator.parseIso8601Duration(item.contentDetails().duration());
                    boolean isShort = durationSeconds <= SHORTS_MAX_DURATION_SECONDS;
                    return format == ChannelVideoFormat.SHORT_FORM ? isShort : !isShort;
                })
                .toList();
    }

    private Map<Integer, String> buildCategoryTitleMap(List<YoutubeDataVideoResponse.Item> items) {
        List<Integer> categoryIds = items.stream()
                .map(item -> item.snippet() != null ? item.snippet().categoryId() : null)
                .filter(StringUtils::hasText)
                .map(id -> {
                    try { return Integer.parseInt(id); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return youtubeCategoryRepository
                .findByYoutubeCategoryIdIn(categoryIds)
                .stream()
                .collect(Collectors.toMap(c -> c.getYoutubeCategoryId(), c -> c.getTitle()));
    }

    private Map<String, String> collectAliasToNameMap(List<YoutubeDataVideoResponse.Item> items) {
        Set<String> candidates = items.stream()
                .filter(item -> item.snippet() != null)
                .flatMap(item -> descriptionTokens(item.snippet().description()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return brandService.resolveAliasToNameMap(candidates);
    }

    private List<ChannelBrandHistoryVideoResponse> buildContent(
            List<YoutubeDataVideoResponse.Item> items,
            Map<Integer, String> categoryTitleMap,
            Map<String, String> aliasToNameMap
    ) {
        return items.stream()
                .map(item -> {
                    String categoryName = resolveCategoryName(item, categoryTitleMap);
                    String videoFormat = resolveVideoFormat(item);
                    List<String> brands = extractBrands(item, aliasToNameMap);

                    return new ChannelBrandHistoryVideoResponse(
                            item.id(),
                            item.snippet() != null ? item.snippet().title() : null,
                            item.snippet() != null && item.snippet().thumbnails() != null
                                    && item.snippet().thumbnails().high() != null
                                    ? item.snippet().thumbnails().high().url() : null,
                            item.snippet() != null ? item.snippet().publishedAt() : null,
                            parseLong(item.statistics() != null ? item.statistics().viewCount() : null),
                            parseLong(item.statistics() != null ? item.statistics().likeCount() : null),
                            parseLong(item.statistics() != null ? item.statistics().commentCount() : null),
                            videoFormat,
                            categoryName,
                            brands
                    );
                })
                .toList();
    }

    private List<String> extractBrands(YoutubeDataVideoResponse.Item item, Map<String, String> aliasToNameMap) {
        if (item.snippet() == null) return List.of();
        return descriptionTokens(item.snippet().description())
                .filter(StringUtils::hasText)
                .map(token -> aliasToNameMap.get(token.toLowerCase()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Stream<String> descriptionTokens(String description) {
        if (!StringUtils.hasText(description)) return Stream.empty();
        return Arrays.stream(description.split("[\\s\\[\\]()#,./|!?:;@\"'\\-]+"));
    }

    private List<ChannelBrandHistoryAnalysisResponse.ContentTypeShare> computeContentTypeDistribution(
            List<YoutubeDataVideoResponse.Item> items
    ) {
        long shortCount = items.stream().filter(item -> FORMAT_SHORT_FORM.equals(resolveVideoFormat(item))).count();
        long longCount = items.size() - shortCount;
        int total = items.size();

        List<ChannelBrandHistoryAnalysisResponse.ContentTypeShare> result = new ArrayList<>();
        if (longCount > 0) {
            result.add(new ChannelBrandHistoryAnalysisResponse.ContentTypeShare(
                    FORMAT_LONG_FORM, (int) longCount, (int) Math.round((double) longCount / total * 100)));
        }
        if (shortCount > 0) {
            result.add(new ChannelBrandHistoryAnalysisResponse.ContentTypeShare(
                    FORMAT_SHORT_FORM, (int) shortCount, (int) Math.round((double) shortCount / total * 100)));
        }
        return result;
    }

    private List<ChannelBrandHistoryAnalysisResponse.CategoryShare> computeCategoryDistribution(
            List<YoutubeDataVideoResponse.Item> items,
            Map<Integer, String> categoryTitleMap
    ) {
        List<Integer> categoryIds = items.stream()
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

        int total = categoryIds.size();
        return categoryIds.stream()
                .collect(Collectors.groupingBy(
                        id -> categoryTitleMap.getOrDefault(id, "기타"),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new ChannelBrandHistoryAnalysisResponse.CategoryShare(
                        e.getKey(),
                        e.getValue().intValue(),
                        (int) Math.round((double) e.getValue() / total * 100)
                ))
                .sorted(Comparator.comparingInt(ChannelBrandHistoryAnalysisResponse.CategoryShare::percentage).reversed())
                .toList();
    }

    private List<ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews> computeAvgViewsByContentType(
            List<YoutubeDataVideoResponse.Item> items
    ) {
        Map<String, List<YoutubeDataVideoResponse.Item>> byFormat = items.stream()
                .collect(Collectors.groupingBy(this::resolveVideoFormat));

        return byFormat.entrySet().stream()
                .map(e -> {
                    List<YoutubeDataVideoResponse.Item> group = e.getValue();
                    long avgViews = (long) group.stream()
                            .mapToLong(item -> parseLong(item.statistics() != null ? item.statistics().viewCount() : null))
                            .average().orElse(0);
                    double avgEngagementRate = group.stream()
                            .mapToDouble(item -> {
                                if (item.statistics() == null) return 0.0;
                                return AnalyticsCalculator.engagementRate(
                                        parseLong(item.statistics().likeCount()),
                                        parseLong(item.statistics().commentCount()),
                                        parseLong(item.statistics().viewCount())
                                );
                            })
                            .average().orElse(0.0);
                    return new ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews(
                            e.getKey(), avgViews, Math.round(avgEngagementRate * 100.0) / 100.0);
                })
                .sorted(Comparator.comparing(ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews::format))
                .toList();
    }

    private String resolveVideoFormat(YoutubeDataVideoResponse.Item item) {
        if (item.contentDetails() == null || !StringUtils.hasText(item.contentDetails().duration())) {
            return FORMAT_LONG_FORM;
        }
        int seconds = (int) AnalyticsCalculator.parseIso8601Duration(item.contentDetails().duration());
        return seconds <= SHORTS_MAX_DURATION_SECONDS ? FORMAT_SHORT_FORM : FORMAT_LONG_FORM;
    }

    private String resolveCategoryName(YoutubeDataVideoResponse.Item item, Map<Integer, String> categoryTitleMap) {
        if (item.snippet() == null || !StringUtils.hasText(item.snippet().categoryId())) {
            return null;
        }
        try {
            int categoryId = Integer.parseInt(item.snippet().categoryId());
            return categoryTitleMap.get(categoryId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String decodePageToken(String cursor, String expectedSortCriteria) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] tokens = decoded.split("\\|", 2);
            if (tokens.length != 2 || !tokens[0].equals(expectedSortCriteria)) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }
            return tokens[1];
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    private String encodeNextCursor(String youtubePageToken, String sortCriteria) {
        if (!StringUtils.hasText(youtubePageToken)) {
            return null;
        }
        String raw = sortCriteria + "|" + youtubePageToken;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorSliceResponse<ChannelBrandHistoryVideoResponse> emptyResponse(ChannelBrandHistorySearchCondition condition) {
        return new CursorSliceResponse<>(
                List.of(),
                new CursorSliceResponse.PageInfo(condition.pageSize(), 0, null, false),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    private ChannelBrandHistoryAnalysisResponse.AdScore computeAdScore(
            String channelId,
            List<YoutubeDataVideoResponse.Item> items
    ) {
        YoutubeDataChannelResponse channelResponse = youtubeDataApiClient.getYoutubeChannels(channelId, CHANNEL_STATS_PARTS);
        if (channelResponse == null || channelResponse.items() == null || channelResponse.items().isEmpty()) {
            return null;
        }
        YoutubeDataChannelResponse.Statistics stats = channelResponse.items().getFirst().statistics();
        if (stats == null) return null;

        long subscriberCount = parseLong(stats.subscriberCount());
        long totalVideoCount = parseLong(stats.videoCount());

        double avgViews = items.stream()
                .mapToLong(item -> parseLong(item.statistics() != null ? item.statistics().viewCount() : null))
                .average().orElse(0.0);

        double cv = computeCoefficientOfVariation(items, avgViews);
        double subscriberHealthRate = subscriberCount > 0 ? (avgViews / subscriberCount) * 100.0 : 0.0;
        double collaborationRate = totalVideoCount > 0 ? ((double) items.size() / totalVideoCount) * 100.0 : 0.0;

        double vsScore = scoreCalculator.viewStabilityScore(cv);
        double shScore = scoreCalculator.subscriberHealthScore(subscriberHealthRate);
        double ceScore = scoreCalculator.sponsorshipExperienceScore(collaborationRate);
        double adScore = scoreCalculator.advertisementScore(vsScore, shScore, ceScore);

        return new ChannelBrandHistoryAnalysisResponse.AdScore(
                (int) Math.round(adScore),
                scoreToLabel(adScore),
                scoreToLabel(vsScore),
                Math.round(cv * 100.0) / 100.0,
                scoreToLabel(shScore),
                Math.round(subscriberHealthRate * 10.0) / 10.0,
                scoreToLabel(ceScore),
                Math.round(collaborationRate * 10.0) / 10.0
        );
    }

    private double computeCoefficientOfVariation(List<YoutubeDataVideoResponse.Item> items, double avgViews) {
        if (items.isEmpty() || avgViews <= 0.0) return 0.0;
        double variance = items.stream()
                .mapToDouble(item -> {
                    double v = parseLong(item.statistics() != null ? item.statistics().viewCount() : null);
                    return Math.pow(v - avgViews, 2);
                })
                .average().orElse(0.0);
        return Math.sqrt(variance) / avgViews;
    }

    private String scoreToLabel(double score) {
        if (score >= 70.0) return "높음";
        if (score >= 40.0) return "보통";
        return "낮음";
    }

    private ChannelBrandHistoryAnalysisResponse emptyAnalysis() {
        return new ChannelBrandHistoryAnalysisResponse(0, List.of(), List.of(), List.of(), null);
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
