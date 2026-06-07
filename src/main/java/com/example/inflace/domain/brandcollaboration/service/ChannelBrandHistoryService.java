package com.example.inflace.domain.brandcollaboration.service;

import com.example.inflace.domain.brand.service.BrandService;
import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.dto.ChannelVideoSliceResult;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.request.ChannelVideoSort;
import com.example.inflace.domain.channel.dto.request.ChannelVideosRequest;
import com.example.inflace.domain.channel.dto.response.ChannelVideosResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.channel.service.insight.InfluencerInsightScoreCalculator;
import com.example.inflace.domain.youtubecategory.repository.YoutubeCategoryRepository;
import com.example.inflace.domain.brandcollaboration.dto.request.ChannelBrandHistorySearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryAnalysisResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryVideoResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoQueryRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.global.response.CustomSort;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final int ANALYSIS_MAX_VIDEOS = 50;
    private static final String FORMAT_LONG_FORM = "LONG_FORM";
    private static final String FORMAT_SHORT_FORM = "SHORT_FORM";

    private final YoutubeCategoryRepository youtubeCategoryRepository;
    private final BrandService brandService;
    private final InfluencerInsightScoreCalculator scoreCalculator;
    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final VideoQueryRepository videoQueryRepository;
    private final VideoRepository videoRepository;

    public CursorSliceResponse<ChannelBrandHistoryVideoResponse> search(String channelId, ChannelBrandHistorySearchCondition condition) {
        Optional<Channel> channelOpt = channelRepository.findByYoutubeChannelId(channelId);
        if (channelOpt.isEmpty()) {
            return emptyResponse(condition);
        }

        String dbCursor = decodePageToken(condition.cursor(), condition.sortCriteriaValue(), condition.sortOrder().name());
        ChannelVideosRequest request = new ChannelVideosRequest(
                null,
                parseRfc3339ToLocalDate(condition.startDate()),
                parseRfc3339ToLocalDate(condition.endDate()),
                toChannelVideoSort(condition.sortCriteriaValue()),
                condition.videoFormatEnum(),
                true,
                dbCursor,
                condition.pageSize(),
                parseCategoryId(condition.categoryId())
        );

        ChannelVideoSliceResult result = videoQueryRepository.findChannelVideos(channelOpt.get().getId(), request);

        if (result.videos().isEmpty()) {
            String nextCursor = encodeNextCursor(result.nextCursor(), condition.sortCriteriaValue(), condition.sortOrder().name());
            return new CursorSliceResponse<>(
                    List.of(),
                    new CursorSliceResponse.PageInfo(condition.pageSize(), 0, nextCursor, result.hasNext()),
                    CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
            );
        }

        List<Long> dbVideoIds = result.videos().stream()
                .map(ChannelVideosResponse.ChannelVideoItem::videoId)
                .toList();
        Map<Long, Video> videoMap = videoRepository.findAllById(dbVideoIds).stream()
                .collect(Collectors.toMap(Video::getId, v -> v));

        Map<Integer, String> categoryTitleMap = buildCategoryTitleMapFromDbVideos(videoMap.values());
        Map<String, String> aliasToNameMap = collectAliasToNameMap(videoMap.values());

        List<ChannelBrandHistoryVideoResponse> content = result.videos().stream()
                .map(item -> {
                    Video video = videoMap.get(item.videoId());
                    if (video == null) return null;
                    String categoryName = video.getCategoryId() != null
                            ? categoryTitleMap.get(video.getCategoryId()) : null;
                    String format = Boolean.TRUE.equals(item.isShort()) ? FORMAT_SHORT_FORM : FORMAT_LONG_FORM;
                    List<String> brands = extractBrandsFromDescription(video.getDescription(), aliasToNameMap);
                    String publishedAt = item.publishedAt() != null
                            ? item.publishedAt().atOffset(ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            : null;
                    return new ChannelBrandHistoryVideoResponse(
                            video.getYoutubeVideoId(),
                            item.title(),
                            item.thumbnailUrl(),
                            publishedAt,
                            item.viewCount() != null ? item.viewCount() : 0L,
                            item.likeCount() != null ? item.likeCount() : 0L,
                            item.commentCount() != null ? item.commentCount() : 0L,
                            format,
                            categoryName,
                            brands
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        String nextCursor = encodeNextCursor(result.nextCursor(), condition.sortCriteriaValue(), condition.sortOrder().name());
        return new CursorSliceResponse<>(
                content,
                new CursorSliceResponse.PageInfo(condition.pageSize(), content.size(), nextCursor, result.hasNext()),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    private ChannelVideoSort toChannelVideoSort(String sortCriteria) {
        return switch (sortCriteria) {
            case "VIEW_COUNT" -> ChannelVideoSort.VIEWS;
            case "LIKE_COUNT" -> ChannelVideoSort.LIKES;
            default -> ChannelVideoSort.LATEST;
        };
    }

    public ChannelBrandHistoryAnalysisResponse analysis(String channelId, ChannelBrandHistorySearchCondition condition) {
        Optional<Channel> channelOpt = channelRepository.findByYoutubeChannelId(channelId);
        if (channelOpt.isEmpty()) {
            return emptyAnalysis();
        }
        Channel channel = channelOpt.get();

        ChannelVideosRequest request = new ChannelVideosRequest(
                null,
                parseRfc3339ToLocalDate(condition.startDate()),
                parseRfc3339ToLocalDate(condition.endDate()),
                ChannelVideoSort.LATEST,
                condition.videoFormatEnum(),
                true,
                null,
                ANALYSIS_MAX_VIDEOS,
                parseCategoryId(condition.categoryId())
        );

        List<ChannelVideosResponse.ChannelVideoItem> videos = videoQueryRepository
                .findChannelVideos(channel.getId(), request)
                .videos();

        if (videos.isEmpty()) {
            return emptyAnalysis();
        }

        List<Long> videoIds = videos.stream().map(ChannelVideosResponse.ChannelVideoItem::videoId).toList();
        Map<Long, Video> videoMap = videoRepository.findAllById(videoIds).stream()
                .collect(Collectors.toMap(Video::getId, v -> v));

        Map<Integer, String> categoryTitleMap = buildCategoryTitleMapFromDbVideos(videoMap.values());

        ChannelBrandHistoryAnalysisResponse.AdScore adScore;
        try {
            adScore = computeAdScore(channel.getId(), videos);
        } catch (Exception e) {
            log.warn("adScore 계산 실패: channelId={}, error={}", channelId, e.getMessage(), e);
            adScore = null;
        }

        return new ChannelBrandHistoryAnalysisResponse(
                videos.size(),
                computeAvgViewsByContentType(videos),
                computeCategoryDistribution(videos, videoMap, categoryTitleMap),
                computeContentTypeDistribution(videos),
                adScore
        );
    }

    private Map<Integer, String> buildCategoryTitleMapFromDbVideos(Collection<Video> videos) {
        List<Integer> categoryIds = videos.stream()
                .map(Video::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return resolveCategoryTitleMap(categoryIds);
    }

    private Map<String, String> collectAliasToNameMap(Collection<Video> videos) {
        Set<String> candidates = videos.stream()
                .flatMap(v -> descriptionTokens(v.getDescription()))
                .filter(StringUtils::hasText)
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return brandService.resolveAliasToNameMap(candidates);
    }

    private List<String> extractBrandsFromDescription(String description, Map<String, String> aliasToNameMap) {
        return descriptionTokens(description)
                .filter(StringUtils::hasText)
                .map(token -> aliasToNameMap.get(token.toLowerCase(Locale.ROOT)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private LocalDate parseRfc3339ToLocalDate(String rfc3339) {
        if (!StringUtils.hasText(rfc3339)) return null;
        try {
            return OffsetDateTime.parse(rfc3339).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseCategoryId(String categoryId) {
        if (!StringUtils.hasText(categoryId)) return null;
        try {
            return Integer.parseInt(categoryId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<Integer, String> resolveCategoryTitleMap(List<Integer> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();
        return youtubeCategoryRepository.findByYoutubeCategoryIdIn(categoryIds).stream()
                .collect(Collectors.toMap(c -> c.getYoutubeCategoryId(), c -> c.getTitle()));
    }

    private Stream<String> descriptionTokens(String description) {
        if (!StringUtils.hasText(description)) return Stream.empty();
        return Arrays.stream(description.split("[\\s\\[\\]()#,./|!?:;@\"'\\-]+"));
    }

    private List<ChannelBrandHistoryAnalysisResponse.ContentTypeShare> computeContentTypeDistribution(
            List<ChannelVideosResponse.ChannelVideoItem> items
    ) {
        long shortCount = items.stream().filter(v -> Boolean.TRUE.equals(v.isShort())).count();
        return buildContentTypeShares(shortCount, items.size() - shortCount);
    }

    private List<ChannelBrandHistoryAnalysisResponse.CategoryShare> computeCategoryDistribution(
            List<ChannelVideosResponse.ChannelVideoItem> items,
            Map<Long, Video> videoMap,
            Map<Integer, String> categoryTitleMap
    ) {
        List<Integer> categoryIds = items.stream()
                .map(v -> videoMap.get(v.videoId()))
                .filter(Objects::nonNull)
                .map(Video::getCategoryId)
                .filter(Objects::nonNull)
                .toList();
        return buildCategoryShares(categoryIds, categoryTitleMap);
    }

    private List<ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews> computeAvgViewsByContentType(
            List<ChannelVideosResponse.ChannelVideoItem> items
    ) {
        Map<String, List<ChannelVideosResponse.ChannelVideoItem>> byFormat = items.stream()
                .collect(Collectors.groupingBy(v -> Boolean.TRUE.equals(v.isShort()) ? FORMAT_SHORT_FORM : FORMAT_LONG_FORM));
        return byFormat.entrySet().stream()
                .map(e -> {
                    List<ChannelVideosResponse.ChannelVideoItem> group = e.getValue();
                    long avgViews = (long) group.stream()
                            .mapToLong(v -> v.viewCount() != null ? v.viewCount() : 0L)
                            .average().orElse(0);
                    double avgEngagementRate = group.stream()
                            .mapToDouble(v -> AnalyticsCalculator.engagementRate(
                                    v.likeCount() != null ? v.likeCount() : 0L,
                                    v.commentCount() != null ? v.commentCount() : 0L,
                                    v.viewCount() != null ? v.viewCount() : 0L))
                            .average().orElse(0.0);
                    return new ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews(
                            e.getKey(), avgViews, Math.round(avgEngagementRate * 100.0) / 100.0);
                })
                .sorted(Comparator.comparing(ChannelBrandHistoryAnalysisResponse.ContentTypeAvgViews::format))
                .toList();
    }

    private String decodePageToken(String cursor, String expectedSortCriteria, String expectedSortOrder) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] tokens = decoded.split("\\|", 3);
            if (tokens.length != 3
                    || !tokens[0].equals(expectedSortCriteria)
                    || !tokens[1].equals(expectedSortOrder)) {
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

    private CursorSliceResponse<ChannelBrandHistoryVideoResponse> emptyResponse(ChannelBrandHistorySearchCondition condition) {
        return new CursorSliceResponse<>(
                List.of(),
                new CursorSliceResponse.PageInfo(condition.pageSize(), 0, null, false),
                CustomSort.of(true, condition.sortCriteriaValue(), condition.sortOrder().name())
        );
    }

    private ChannelBrandHistoryAnalysisResponse.AdScore computeAdScore(
            Long channelId,
            List<ChannelVideosResponse.ChannelVideoItem> items
    ) {
        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId).orElse(null);
        if (channelStats == null) return null;

        long subscriberCount = channelStats.getSubscriberCount();
        long totalVideoCount = channelStats.getTotalVideoCount() != null ? channelStats.getTotalVideoCount() : 0L;

        double avgViews = items.stream()
                .mapToLong(v -> v.viewCount() != null ? v.viewCount() : 0L)
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

    private double computeCoefficientOfVariation(List<ChannelVideosResponse.ChannelVideoItem> items, double avgViews) {
        if (items.isEmpty() || avgViews <= 0.0) return 0.0;
        double variance = items.stream()
                .mapToDouble(item -> Math.pow((item.viewCount() != null ? item.viewCount() : 0L) - avgViews, 2))
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

    private List<ChannelBrandHistoryAnalysisResponse.ContentTypeShare> buildContentTypeShares(long shortCount, long longCount) {
        int total = (int) (shortCount + longCount);
        List<ChannelBrandHistoryAnalysisResponse.ContentTypeShare> result = new ArrayList<>();
        if (longCount > 0) result.add(new ChannelBrandHistoryAnalysisResponse.ContentTypeShare(
                FORMAT_LONG_FORM, (int) longCount, (int) Math.round((double) longCount / total * 100)));
        if (shortCount > 0) result.add(new ChannelBrandHistoryAnalysisResponse.ContentTypeShare(
                FORMAT_SHORT_FORM, (int) shortCount, (int) Math.round((double) shortCount / total * 100)));
        return result;
    }

    private List<ChannelBrandHistoryAnalysisResponse.CategoryShare> buildCategoryShares(
            List<Integer> categoryIds, Map<Integer, String> categoryTitleMap
    ) {
        if (categoryIds.isEmpty()) return List.of();
        int total = categoryIds.size();
        return categoryIds.stream()
                .collect(Collectors.groupingBy(id -> categoryTitleMap.getOrDefault(id, "기타"), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChannelBrandHistoryAnalysisResponse.CategoryShare(
                        e.getKey(), e.getValue().intValue(),
                        (int) Math.round((double) e.getValue() / total * 100)))
                .sorted(Comparator.comparingInt(ChannelBrandHistoryAnalysisResponse.CategoryShare::percentage).reversed())
                .toList();
    }

}
