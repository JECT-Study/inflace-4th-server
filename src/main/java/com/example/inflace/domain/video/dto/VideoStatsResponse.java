package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.global.util.AnalyticsCalculator;

import java.time.LocalDateTime;

import static com.example.inflace.global.util.AnalyticsParser.safeDoubleValue;

public record VideoStatsResponse(
        LocalDateTime collectedAt,
        StatValue viewCount,
        StatValue likeCount,
        StatValue commentCount,
        StatValue shareCount,
        StatValue subscribersGained,
        StatValue avd,
        StatValue engagementRate,
        StatValue newViewerRate,
        StatValue outlier,
        StatValue vph
) {
    public record StatValue(
            Double value,  // Jackson 직렬화할 때 타입 맞추기 위해 Double로 통일
            Double changeRate
    ) {
    }

    public static VideoStatsResponse from(VideoStats stats, VideoAnalytics analytics, VideoStatsChannelAverages averages) {
        Double viewCount = safeDoubleValue(stats.getViewCount());
        Double likeCount = safeDoubleValue(stats.getLikeCount());
        Double commentCount = safeDoubleValue(stats.getCommentCount());
        Double shareCount = safeDoubleValue(analytics != null ? analytics.getShareCount() : null);
        Double subscribersGained = safeDoubleValue(analytics != null ? analytics.getSubscribersGained() : null);
        Double avd = safeDoubleValue(analytics != null ? analytics.getAvgWatchDuration() : null);
        Double engagementRate = AnalyticsCalculator.engagementRate(
                stats.getLikeCount(),
                stats.getCommentCount(),
                stats.getViewCount()
        );
        Double newViewerRate = AnalyticsCalculator.newViewerRate(
                analytics != null ? analytics.getUnsubscribedViewCount() : null,
                stats.getViewCount()
        );
        Double outlier = safeDoubleValue(stats.getOutlierScore());
        Double vph = safeDoubleValue(stats.getVph());

        return new VideoStatsResponse(
                stats.getCollectedAt(),
                new StatValue(viewCount, changeRate(viewCount, averages != null ? averages.viewCount() : null)),
                new StatValue(likeCount, changeRate(likeCount, averages != null ? averages.likeCount() : null)),
                new StatValue(commentCount, changeRate(commentCount, averages != null ? averages.commentCount() : null)),
                new StatValue(shareCount, changeRate(shareCount, averages != null ? averages.shareCount() : null)),
                new StatValue(subscribersGained, changeRate(subscribersGained, averages != null ? averages.subscribersGained() : null)),
                new StatValue(avd, changeRate(avd, averages != null ? averages.avd() : null)),
                new StatValue(engagementRate, changeRate(engagementRate, averages != null ? averages.engagementRate() : null)),
                new StatValue(newViewerRate, changeRate(newViewerRate, averages != null ? averages.newViewerRate() : null)),
                new StatValue(outlier, changeRate(outlier, averages != null ? averages.outlier() : null)),
                new StatValue(vph, changeRate(vph, averages != null ? averages.vph() : null))
        );
    }

    private static double changeRate(Double value, Double average) {
        if (average == null || average == 0.0) {
            return 0.0;
        }
        return Math.round(((safeDoubleValue(value) - average) / average) * 10000.0) / 100.0;
    }
}
