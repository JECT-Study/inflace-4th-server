package com.example.inflace.domain.channel.service.sync;

import static com.example.inflace.global.util.AnalyticsParser.toDouble;
import static com.example.inflace.global.util.AnalyticsParser.toLong;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.domain.SubscriberLog;
import com.example.inflace.domain.channel.repository.ChannelAnalyticsRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.channel.repository.SubscriberLogRepository;
import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeChannelAnalyticsSyncService {

    private static final int ANALYTICS_END_DATE_OFFSET_DAYS = 3;
    private static final int SUBSCRIBER_LOG_MAX_BACKFILL_DAYS = 365;

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

    private final ChannelAnalyticsRepository channelAnalyticsRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final SubscriberLogRepository subscriberLogRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;
    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;

    @Transactional
    public void syncAnalytics(String googleId, Channel channel, List<Video> videos) {
        LocalDate endDate = resolveEndDate();
        syncChannelAnalytics(googleId, channel, endDate);
        syncSubscriberLogs(googleId, channel, endDate);
        syncVideoAnalytics(googleId, videos, endDate);
        syncAudienceRetention(googleId, videos, endDate);
    }

    private void syncChannelAnalytics(String googleId, Channel channel, LocalDate endDate) {
        LocalDate startDate = resolveStartDate(channel.getYoutubePublishedAt(), endDate);
        LocalDateTime now = LocalDateTime.now();
        YoutubeAnalyticsVideoRequest summaryRequest = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                CHANNEL_METRICS,
                null,
                null,
                null
        );
        Map<String, Object> summary = querySingleRow(googleId, summaryRequest);
        Map<String, Long> subscriberViews = querySubscriberViews(channel, googleId, startDate, endDate);
        Map<String, Double> audienceGender = queryDistribution(googleId, startDate, endDate, "gender");
        Map<String, Double> audienceAge = queryDistribution(googleId, startDate, endDate, "ageGroup");
        Map<String, Double> audienceCountry = queryCountryDistribution(googleId, startDate, endDate);

        Long views = summary.isEmpty() ? null : toLong(summary.get("views"));
        Long watchedMinutes = summary.isEmpty() ? null : toLong(summary.get("estimatedMinutesWatched"));
        Integer averageViewDurationSeconds = summary.isEmpty()
                ? null
                : toInteger(summary.get("averageViewDuration"));

        Optional<ChannelAnalytics> existing = channelAnalyticsRepository.findByChannel_Id(channel.getId());
        if (existing.isPresent()) {
            existing.get().update(
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
            return;
        }

        channelAnalyticsRepository.save(ChannelAnalytics.builder()
                .channel(channel)
                .startDate(startDate)
                .endDate(endDate)
                .collectedAt(now)
                .views(views)
                .subscriberViewCount(subscriberViews.get("SUBSCRIBED"))
                .nonSubscriberViewCount(subscriberViews.get("UNSUBSCRIBED"))
                .watchedMinutes(watchedMinutes)
                .averageViewDurationSeconds(averageViewDurationSeconds)
                .audienceGender(audienceGender)
                .audienceAge(audienceAge)
                .audienceCountry(audienceCountry)
                .build());
    }

    private void syncSubscriberLogs(String googleId, Channel channel, LocalDate endDate) {
        LocalDate startDate = resolveSubscriberLogStartDate(channel, endDate);
        if (startDate.isAfter(endDate)) {
            return;
        }

        YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                startDate,
                endDate,
                SUBSCRIBER_LOG_METRICS,
                null,
                "day",
                null
        );
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
        if (response.rows() == null || response.rows().isEmpty()) {
            return;
        }

        Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
        Long subscriberCount = channelStatsRepository.findByChannel_Id(channel.getId())
                .map(ChannelStats::getSubscriberCount)
                .orElse(null);

        List<List<Object>> sortedRows = new ArrayList<>(response.rows());
        sortedRows.sort(Comparator.comparing(row -> LocalDate.parse((String) row.get(indexMap.get("day")))));

        List<SubscriberLog> logs = new ArrayList<>();
        Long runningSubscriberCount = subscriberCount;
        for (int i = sortedRows.size() - 1; i >= 0; i--) {
            List<Object> row = sortedRows.get(i);
            LocalDate recordedDate = LocalDate.parse((String) row.get(indexMap.get("day")));
            Long subscribersGained = toLong(row.get(indexMap.get("subscribersGained")));
            Long subscribersLost = toLong(row.get(indexMap.get("subscribersLost")));

            logs.add(SubscriberLog.builder()
                    .channel(channel)
                    .subscriberCount(runningSubscriberCount)
                    .subscribersGained(subscribersGained)
                    .subscribersLost(subscribersLost)
                    .recordedDate(recordedDate)
                    .build());

            if (runningSubscriberCount != null) {
                runningSubscriberCount = runningSubscriberCount
                        - (subscribersGained != null ? subscribersGained : 0L)
                        + (subscribersLost != null ? subscribersLost : 0L);
            }
        }
        Collections.reverse(logs);

        if (!logs.isEmpty()) {
            subscriberLogRepository.saveAll(logs);
        }
    }

    private void syncVideoAnalytics(String googleId, List<Video> videos, LocalDate endDate) {
        for (Video video : videos) {
            LocalDate startDate = resolveStartDate(video.getPublishedAt(), endDate);
            if (startDate.isAfter(endDate)) {
                continue;
            }

            YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                    startDate,
                    endDate,
                    VIDEO_METRICS,
                    video.getYoutubeVideoId(),
                    "video",
                    null
            );
            Map<String, Object> summary = querySingleRow(googleId, request);

            VideoAnalytics analytics = videoAnalyticsRepository.findByVideoId(video.getId())
                    .orElseGet(() -> VideoAnalytics.builder().video(video).build());

            if (!summary.isEmpty()) {
                analytics.update(
                        toLong(summary.get("shares")),
                        toLong(summary.get("subscribersGained")),
                        toDouble(summary.get("annotationClickThroughRate")),
                        toDouble(summary.get("averageViewDuration")),
                        toDouble(summary.get("averageViewPercentage")),
                        LocalDateTime.now()
                );
                if (analytics.getId() == null) {
                    videoAnalyticsRepository.save(analytics);
                }
            } else if (analytics.getId() == null) {
                continue;
            }

            syncUnsubscribedVideoAnalytics(googleId, video, startDate, endDate, analytics);
        }
    }

    private void syncUnsubscribedVideoAnalytics(
            String googleId,
            Video video,
            LocalDate startDate,
            LocalDate endDate,
            VideoAnalytics analytics
    ) {
        try {
            YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                    startDate,
                    endDate,
                    VIDEO_UNSUBSCRIBED_METRICS,
                    video.getYoutubeVideoId(),
                    "subscribedStatus",
                    null
            );
            YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
            if (response.rows() == null || response.rows().isEmpty()) {
                return;
            }

            Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
            List<Object> unsubscribedRow = response.rows().stream()
                    .filter(row -> "UNSUBSCRIBED".equals(row.get(indexMap.get("subscribedStatus"))))
                    .findFirst()
                    .orElse(null);
            if (unsubscribedRow == null) {
                return;
            }

            Long unsubscribedViewCount = toLong(unsubscribedRow.get(indexMap.get("views")));
            Double unsubscribedViewerPercentage = calculateUnsubscribedViewerPercentage(video.getId(), unsubscribedViewCount);

            analytics.updateUnsubscribed(unsubscribedViewCount, unsubscribedViewerPercentage);
        } catch (ApiException e) {
            log.warn("Failed to sync unsubscribed video analytics. videoId={} youtubeVideoId={} startDate={} endDate={}",
                    video.getId(), video.getYoutubeVideoId(), startDate, endDate, e);
        }
    }

    private void syncAudienceRetention(String googleId, List<Video> videos, LocalDate endDate) {
        for (Video video : videos) {
            LocalDate startDate = resolveStartDate(video.getPublishedAt(), endDate);
            if (startDate.isAfter(endDate)) {
                continue;
            }

            YoutubeAnalyticsVideoRequest request = new YoutubeAnalyticsVideoRequest(
                    startDate,
                    endDate,
                    VIDEO_RETENTION_METRICS,
                    video.getYoutubeVideoId(),
                    "elapsedVideoTimeRatio",
                    null
            );
            YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
            if (response.rows() == null || response.rows().isEmpty()) {
                continue;
            }

            Map<String, Integer> indexMap = buildIndexMap(response.columnHeaders());
            LocalDateTime now = LocalDateTime.now();

            audienceRetentionRepository.deleteByVideoId(video.getId());

            List<AudienceRetention> retentions = response.rows().stream()
                    .map(row -> AudienceRetention.builder()
                            .video(video)
                            .timeRatio(toDouble(row.get(indexMap.get("elapsedVideoTimeRatio"))))
                            .retentionRate(toDouble(row.get(indexMap.get("audienceWatchRatio"))))
                            .collectedAt(now)
                            .build())
                    .toList();
            audienceRetentionRepository.saveAll(retentions);

            double relativeRetentionAvg = response.rows().stream()
                    .mapToDouble(row -> toDouble(row.get(indexMap.get("relativeRetentionPerformance"))))
                    .average()
                    .orElse(0.0);

            videoAnalyticsRepository.findByVideoId(video.getId())
                    .ifPresent(analytics -> analytics.updateRelativeRetention(relativeRetentionAvg));
        }
    }

    private Map<String, Long> querySubscriberViews(
            Channel channel,
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
                    channel.getId(), channel.getYoutubeChannelId(), startDate, endDate, e);
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
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
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
        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);
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

    private LocalDate resolveSubscriberLogStartDate(Channel channel, LocalDate endDate) {
        LocalDate oldestAllowedDate = endDate.minusDays(SUBSCRIBER_LOG_MAX_BACKFILL_DAYS - 1L);
        Optional<SubscriberLog> latestLog = subscriberLogRepository.findTopByChannel_IdOrderByRecordedDateDesc(channel.getId());
        if (latestLog.isPresent()) {
            LocalDate nextDate = latestLog.get().getRecordedDate().plusDays(1);
            return nextDate.isBefore(oldestAllowedDate) ? oldestAllowedDate : nextDate;
        }

        LocalDate channelStartDate = resolveStartDate(channel.getYoutubePublishedAt(), endDate);
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

    private Double calculateUnsubscribedViewerPercentage(Long videoId, Long unsubscribedViewCount) {
        if (unsubscribedViewCount == null || unsubscribedViewCount <= 0) {
            return 0.0;
        }

        Long totalViewCount = videoStatsRepository.findByVideoId(videoId)
                .map(VideoStats::getViewCount)
                .orElse(null);
        if (totalViewCount == null || totalViewCount <= 0) {
            return 0.0;
        }

        return round((unsubscribedViewCount * 100.0) / totalViewCount, 6);
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

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
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
