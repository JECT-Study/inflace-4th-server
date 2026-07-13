package com.example.inflace.domain.channel.service.sync;

import static com.example.inflace.global.util.AnalyticsParser.toDouble;
import static com.example.inflace.global.util.AnalyticsParser.toLong;

import com.example.inflace.domain.channel.dto.sync.AnalyticsSyncContext;
import com.example.inflace.domain.channel.dto.sync.YoutubeAnalyticsSyncData;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.util.AnalyticsParser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeChannelAnalyticsFetchService {

    private static final int ANALYTICS_END_DATE_OFFSET_DAYS = 3;
    private static final int SUBSCRIBER_LOG_MAX_BACKFILL_DAYS = 365;
    private static final int VIDEO_ANALYTICS_BATCH_SIZE = 4;

    private static final List<String> CHANNEL_METRICS = List.of(
            "views",
            "estimatedMinutesWatched",
            "averageViewDuration"
    );
    private static final List<String> CHANNEL_SUBSCRIBER_METRICS = List.of("views");
    private static final List<String> CHANNEL_DISTRIBUTION_METRICS = List.of("viewerPercentage");
    private static final List<String> CHANNEL_COUNTRY_METRICS = List.of("views");
    private static final List<String> SUBSCRIBER_LOG_METRICS = List.of("subscribersGained", "subscribersLost");
    private static final List<String> VIDEO_METRICS = List.of(
            "shares",
            "averageViewDuration",
            "averageViewPercentage",
            "subscribersGained",
            "annotationClickThroughRate"
    );
    private static final List<String> VIDEO_UNSUBSCRIBED_METRICS = List.of("views");
    private static final List<String> VIDEO_RETENTION_METRICS = List.of(
            "audienceWatchRatio",
            "relativeRetentionPerformance"
    );

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;
    private final ExecutorService externalApiExecutor;

    public YoutubeAnalyticsSyncData fetchAnalytics(String googleId, AnalyticsSyncContext context) {
        LocalDate endDate = resolveEndDate();
        CompletableFuture<YoutubeAnalyticsSyncData.ChannelAnalyticsData> channelAnalyticsFuture = CompletableFuture
                .supplyAsync(() -> fetchChannelAnalytics(googleId, context, endDate), externalApiExecutor);
        CompletableFuture<List<YoutubeAnalyticsSyncData.SubscriberLogData>> subscriberLogsFuture = CompletableFuture
                .supplyAsync(() -> fetchSubscriberLogs(googleId, context, endDate), externalApiExecutor);

        return new YoutubeAnalyticsSyncData(
                channelAnalyticsFuture.join(),
                subscriberLogsFuture.join(),
                fetchVideoAnalytics(googleId, context.videos(), endDate),
                fetchAudienceRetention(googleId, context.videos(), endDate)
        );
    }

    private YoutubeAnalyticsSyncData.ChannelAnalyticsData fetchChannelAnalytics(
            String googleId,
            AnalyticsSyncContext context,
            LocalDate endDate
    ) {
        LocalDate startDate = resolveStartDate(context.youtubePublishedAt(), endDate);
        LocalDateTime now = LocalDateTime.now();
        YoutubeAnalyticsVideoRequest summaryRequest = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                CHANNEL_METRICS,
                null,
                null,
                null
        );
        Map<String, Object> summary = querySingleRowOrEmpty(
                googleId,
                summaryRequest,
                "channel summary",
                context.channelId(),
                context.youtubeChannelId(),
                startDate,
                endDate
        );
        Map<String, Long> subscriberViews = querySubscriberViews(context, googleId, startDate, endDate);
        Map<String, Double> audienceGender = queryDistribution(googleId, startDate, endDate, "gender");
        Map<String, Double> audienceAge = queryDistribution(googleId, startDate, endDate, "ageGroup");
        Map<String, Double> audienceCountry = queryCountryDistribution(googleId, startDate, endDate);

        Long views = summary.isEmpty() ? null : toLong(summary.get("views"));
        Long watchedMinutes = summary.isEmpty() ? null : toLong(summary.get("estimatedMinutesWatched"));
        Integer averageViewDurationSeconds = summary.isEmpty()
                ? null
                : toInteger(summary.get("averageViewDuration"));

        return new YoutubeAnalyticsSyncData.ChannelAnalyticsData(
                !summary.isEmpty(),
                startDate,
                endDate,
                now,
                views,
                subscriberViews.get("SUBSCRIBED"),
                subscriberViews.get("UNSUBSCRIBED"),
                watchedMinutes,
                averageViewDurationSeconds,
                audienceGender,
                audienceAge,
                audienceCountry
        );
    }

    private List<YoutubeAnalyticsSyncData.SubscriberLogData> fetchSubscriberLogs(
            String googleId,
            AnalyticsSyncContext context,
            LocalDate endDate
    ) {
        LocalDate startDate = resolveSubscriberLogStartDate(context, endDate);
        if (startDate.isAfter(endDate)) {
            return List.of();
        }

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                SUBSCRIBER_LOG_METRICS,
                null,
                "day",
                null
        );
        YoutubeAnalyticsVideoResponse response;
        try {
            response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        } catch (ApiException e) {
            log.warn("Failed to sync subscriber logs. channelId={} youtubeChannelId={} startDate={} endDate={}",
                    context.channelId(), context.youtubeChannelId(), startDate, endDate, e);
            return List.of();
        }
        if (response.rows() == null || response.rows().isEmpty()) {
            return List.of();
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
        List<List<Object>> sortedRows = new ArrayList<>(response.rows());
        sortedRows.sort(Comparator.comparing(row -> LocalDate.parse((String) row.get(indexMap.get("day")))));

        List<YoutubeAnalyticsSyncData.SubscriberLogData> logs = new ArrayList<>();
        Long runningSubscriberCount = context.subscriberCount();
        for (int i = sortedRows.size() - 1; i >= 0; i--) {
            List<Object> row = sortedRows.get(i);
            LocalDate recordedDate = LocalDate.parse((String) row.get(indexMap.get("day")));
            Long subscribersGained = toLong(row.get(indexMap.get("subscribersGained")));
            Long subscribersLost = toLong(row.get(indexMap.get("subscribersLost")));

            logs.add(new YoutubeAnalyticsSyncData.SubscriberLogData(
                    recordedDate,
                    runningSubscriberCount,
                    subscribersGained,
                    subscribersLost
            ));

            if (runningSubscriberCount != null) {
                runningSubscriberCount = runningSubscriberCount
                        - (subscribersGained != null ? subscribersGained : 0L)
                        + (subscribersLost != null ? subscribersLost : 0L);
            }
        }
        Collections.reverse(logs);

        return logs;
    }

    private List<YoutubeAnalyticsSyncData.VideoAnalyticsData> fetchVideoAnalytics(
            String googleId,
            List<AnalyticsSyncContext.VideoContext> videos,
            LocalDate endDate
    ) {
        List<YoutubeAnalyticsSyncData.VideoAnalyticsData> result = new ArrayList<>();
        for (List<AnalyticsSyncContext.VideoContext> batch : chunked(videos, VIDEO_ANALYTICS_BATCH_SIZE)) {
            List<CompletableFuture<YoutubeAnalyticsSyncData.VideoAnalyticsData>> futures = batch.stream()
                    .map(video -> CompletableFuture
                            .supplyAsync(() -> fetchVideoAnalyticsForVideo(googleId, video, endDate), externalApiExecutor)
                            .exceptionally(exception -> {
                                log.warn("Failed to schedule video analytics sync. videoId={} youtubeVideoId={}",
                                        video.videoId(), video.youtubeVideoId(), exception);
                                return defaultVideoAnalyticsData(video.videoId(), !video.existingVideoAnalytics());
                            }))
                    .toList();

            result.addAll(futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList());
        }
        return result;
    }

    private YoutubeAnalyticsSyncData.VideoAnalyticsData fetchVideoAnalyticsForVideo(
            String googleId,
            AnalyticsSyncContext.VideoContext video,
            LocalDate endDate
    ) {
        LocalDate startDate = resolveStartDate(video.publishedAt(), endDate);
        if (startDate.isAfter(endDate)) {
            return null;
        }

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                VIDEO_METRICS,
                video.youtubeVideoId(),
                "video",
                null
        );
        Map<String, Object> summary;
        try {
            summary = querySingleRow(googleId, request);
        } catch (ApiException e) {
            log.warn("Failed to sync video analytics summary. videoId={} youtubeVideoId={} startDate={} endDate={}",
                    video.videoId(), video.youtubeVideoId(), startDate, endDate, e);
            return defaultVideoAnalyticsData(video.videoId(), !video.existingVideoAnalytics());
        }

        if (summary.isEmpty() && !video.existingVideoAnalytics()) {
            return null;
        }

        return fetchUnsubscribedVideoAnalytics(
                googleId,
                video,
                startDate,
                endDate,
                summary
        );
    }

    private YoutubeAnalyticsSyncData.VideoAnalyticsData fetchUnsubscribedVideoAnalytics(
            String googleId,
            AnalyticsSyncContext.VideoContext video,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Object> summary
    ) {
        Long unsubscribedViewCount = null;
        Double unsubscribedViewerPercentage = null;
        try {
            YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                    startDate,
                    endDate,
                    VIDEO_UNSUBSCRIBED_METRICS,
                    video.youtubeVideoId(),
                    "subscribedStatus",
                    null
            );
            YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
            if (response.rows() == null || response.rows().isEmpty()) {
                return toVideoAnalyticsData(video.videoId(), summary, null, null);
            }

            Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
            List<Object> unsubscribedRow = response.rows().stream()
                    .filter(row -> "UNSUBSCRIBED".equals(row.get(indexMap.get("subscribedStatus"))))
                    .findFirst()
                    .orElse(null);
            if (unsubscribedRow == null) {
                return toVideoAnalyticsData(video.videoId(), summary, null, null);
            }

            unsubscribedViewCount = toLong(unsubscribedRow.get(indexMap.get("views")));
            unsubscribedViewerPercentage = calculateUnsubscribedViewerPercentage(video, unsubscribedViewCount);
        } catch (ApiException e) {
            log.warn("Failed to sync unsubscribed video analytics. videoId={} youtubeVideoId={} startDate={} endDate={}",
                    video.videoId(), video.youtubeVideoId(), startDate, endDate, e);
        }
        return toVideoAnalyticsData(video.videoId(), summary, unsubscribedViewCount, unsubscribedViewerPercentage);
    }

    private YoutubeAnalyticsSyncData.VideoAnalyticsData toVideoAnalyticsData(
            Long videoId,
            Map<String, Object> summary,
            Long unsubscribedViewCount,
            Double unsubscribedViewerPercentage
    ) {
        boolean updateSummary = !summary.isEmpty();
        return new YoutubeAnalyticsSyncData.VideoAnalyticsData(
                videoId,
                updateSummary,
                updateSummary ? toLong(summary.get("shares")) : null,
                updateSummary ? toLong(summary.get("subscribersGained")) : null,
                updateSummary ? toDouble(summary.get("annotationClickThroughRate")) : null,
                updateSummary ? toDouble(summary.get("averageViewDuration")) : null,
                updateSummary ? toDouble(summary.get("averageViewPercentage")) : null,
                updateSummary ? LocalDateTime.now() : null,
                unsubscribedViewCount,
                unsubscribedViewerPercentage
        );
    }

    private List<YoutubeAnalyticsSyncData.AudienceRetentionData> fetchAudienceRetention(
            String googleId,
            List<AnalyticsSyncContext.VideoContext> videos,
            LocalDate endDate
    ) {
        List<YoutubeAnalyticsSyncData.AudienceRetentionData> result = new ArrayList<>();
        for (List<AnalyticsSyncContext.VideoContext> batch : chunked(videos, VIDEO_ANALYTICS_BATCH_SIZE)) {
            List<CompletableFuture<List<YoutubeAnalyticsSyncData.AudienceRetentionData>>> futures = batch.stream()
                    .map(video -> CompletableFuture
                            .supplyAsync(() -> fetchAudienceRetentionForVideo(googleId, video, endDate), externalApiExecutor)
                            .exceptionally(exception -> {
                                log.warn("Failed to schedule audience retention sync. videoId={} youtubeVideoId={}",
                                        video.videoId(), video.youtubeVideoId(), exception);
                                return List.of();
                            }))
                    .toList();

            result.addAll(futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList());
        }
        return result;
    }

    private List<YoutubeAnalyticsSyncData.AudienceRetentionData> fetchAudienceRetentionForVideo(
            String googleId,
            AnalyticsSyncContext.VideoContext video,
            LocalDate endDate
    ) {
        LocalDate startDate = resolveStartDate(video.publishedAt(), endDate);
        if (startDate.isAfter(endDate)) {
            return List.of();
        }

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                VIDEO_RETENTION_METRICS,
                video.youtubeVideoId(),
                "elapsedVideoTimeRatio",
                null
        );
        try {
            YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
            if (response.rows() == null || response.rows().isEmpty()) {
                return List.of();
            }

            Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
            LocalDateTime now = LocalDateTime.now();

            List<YoutubeAnalyticsSyncData.AudienceRetentionPointData> retentions = response.rows().stream()
                    .map(row -> new YoutubeAnalyticsSyncData.AudienceRetentionPointData(
                            toDouble(row.get(indexMap.get("elapsedVideoTimeRatio"))),
                            toDouble(row.get(indexMap.get("audienceWatchRatio"))),
                            now
                    ))
                    .toList();

            double relativeRetentionAvg = response.rows().stream()
                    .mapToDouble(row -> toDouble(row.get(indexMap.get("relativeRetentionPerformance"))))
                    .average()
                    .orElse(0.0);

            return List.of(new YoutubeAnalyticsSyncData.AudienceRetentionData(
                    video.videoId(),
                    retentions,
                    relativeRetentionAvg
            ));
        } catch (ApiException e) {
            log.warn("Failed to sync audience retention. videoId={} youtubeVideoId={} startDate={} endDate={}",
                    video.videoId(), video.youtubeVideoId(), startDate, endDate, e);
            return List.of();
        }
    }

    private Map<String, Long> querySubscriberViews(
            AnalyticsSyncContext context,
            String googleId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        try {
            YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                    startDate,
                    endDate,
                    CHANNEL_SUBSCRIBER_METRICS,
                    null,
                    "subscribedStatus",
                    null
            );
            YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
            if (response.rows() == null || response.rows().isEmpty()) {
                return Map.of();
            }

            Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
            Map<String, Long> result = new LinkedHashMap<>();
            for (List<Object> row : response.rows()) {
                Object status = row.get(indexMap.get("subscribedStatus"));
                if (status == null) {
                    continue;
                }
                result.put(status.toString(), toLong(row.get(indexMap.get("views"))));
            }
            return result;
        } catch (ApiException e) {
            log.warn("Failed to sync subscribedStatus analytics. channelId={} youtubeChannelId={} startDate={} endDate={}",
                    context.channelId(), context.youtubeChannelId(), startDate, endDate, e);
            return Map.of();
        }
    }

    private Map<String, Double> queryDistribution(
            String googleId,
            LocalDate startDate,
            LocalDate endDate,
            String dimension
    ) {
        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                CHANNEL_DISTRIBUTION_METRICS,
                null,
                dimension,
                null
        );
        YoutubeAnalyticsVideoResponse response;
        try {
            response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        } catch (ApiException e) {
            log.warn("Failed to sync channel distribution analytics. dimension={} startDate={} endDate={}",
                    dimension, startDate, endDate, e);
            return Map.of();
        }
        if (response.rows() == null || response.rows().isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
        return response.rows().stream()
                .filter(row -> row.get(indexMap.get(dimension)) != null)
                .collect(Collectors.toMap(
                        row -> row.get(indexMap.get(dimension)).toString(),
                        row -> toDouble(row.get(indexMap.get("viewerPercentage"))),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Double> queryCountryDistribution(
            String googleId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                CHANNEL_COUNTRY_METRICS,
                null,
                "country",
                null
        );
        YoutubeAnalyticsVideoResponse response;
        try {
            response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        } catch (ApiException e) {
            log.warn("Failed to sync channel country analytics. startDate={} endDate={}",
                    startDate, endDate, e);
            return Map.of();
        }
        if (response.rows() == null || response.rows().isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
        Map<String, Long> viewsByCountry = response.rows().stream()
                .filter(row -> row.get(indexMap.get("country")) != null)
                .collect(Collectors.toMap(
                        row -> row.get(indexMap.get("country")).toString(),
                        row -> toLong(row.get(indexMap.get("views"))),
                        Long::sum,
                        LinkedHashMap::new
                ));

        long totalViews = viewsByCountry.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        if (totalViews <= 0) {
            return Map.of();
        }

        Map<String, Double> distribution = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : viewsByCountry.entrySet()) {
            distribution.put(entry.getKey(), round((entry.getValue() * 100.0) / totalViews, 6));
        }
        return distribution;
    }

    private LocalDate resolveSubscriberLogStartDate(AnalyticsSyncContext context, LocalDate endDate) {
        LocalDate oldestAllowedDate = endDate.minusDays(SUBSCRIBER_LOG_MAX_BACKFILL_DAYS - 1L);
        if (context.latestSubscriberLogDate() != null) {
            LocalDate nextDate = context.latestSubscriberLogDate().plusDays(1);
            return nextDate.isBefore(oldestAllowedDate) ? oldestAllowedDate : nextDate;
        }

        LocalDate channelStartDate = resolveStartDate(context.youtubePublishedAt(), endDate);
        return channelStartDate.isBefore(oldestAllowedDate) ? oldestAllowedDate : channelStartDate;
    }

    private LocalDate resolveStartDate(LocalDateTime publishedAt, LocalDate endDate) {
        if (publishedAt == null) {
            return endDate;
        }

        LocalDate startDate = publishedAt.toLocalDate();
        return startDate.isAfter(endDate) ? endDate : startDate;
    }

    private LocalDate resolveEndDate() {
        return LocalDate.now().minusDays(ANALYTICS_END_DATE_OFFSET_DAYS);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Math.toIntExact(Math.round(AnalyticsParser.toDouble(value)));
    }

    private Double calculateUnsubscribedViewerPercentage(
            AnalyticsSyncContext.VideoContext video,
            Long unsubscribedViewCount
    ) {
        if (unsubscribedViewCount == null || unsubscribedViewCount <= 0) {
            return 0.0;
        }
        if (video.viewCount() == null || video.viewCount() <= 0) {
            return 0.0;
        }

        return round((unsubscribedViewCount * 100.0) / video.viewCount(), 6);
    }

    private Map<String, Object> querySingleRow(String googleId, YoutubeAnalyticsVideoRequest request) {
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        if (response.rows() == null || response.rows().isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
        List<Object> row = response.rows().get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : indexMap.entrySet()) {
            result.put(entry.getKey(), row.get(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> querySingleRowOrEmpty(
            String googleId,
            YoutubeAnalyticsVideoRequest request,
            String label,
            Long channelId,
            String youtubeChannelId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        try {
            return querySingleRow(googleId, request);
        } catch (ApiException e) {
            log.warn("Failed to sync {} analytics. channelId={} youtubeChannelId={} startDate={} endDate={}",
                    label, channelId, youtubeChannelId, startDate, endDate, e);
            return Map.of();
        }
    }

    private YoutubeAnalyticsSyncData.VideoAnalyticsData defaultVideoAnalyticsData(Long videoId, boolean createPlaceholder) {
        return new YoutubeAnalyticsSyncData.VideoAnalyticsData(
                videoId,
                createPlaceholder,
                null,
                null,
                null,
                null,
                null,
                createPlaceholder ? LocalDateTime.now() : null,
                null,
                null
        );
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private <T> List<List<T>> chunked(List<T> values, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            chunks.add(values.subList(start, Math.min(values.size(), start + size)));
        }
        return chunks;
    }

    private Map<String, Integer> buildIndexMap(List<YoutubeAnalyticsVideoResponse.ColumnHeader> headers) {
        return IntStream.range(0, headers.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> headers.get(i).name(),
                        i -> i
                ));
    }
}
