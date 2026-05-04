package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

@Component
public class InfluencerInsightCalculator {

    private static final int RECENT_WINDOW_DAYS = 30;
    public GetInfluencerInsightResponse calculate(
            Channel channel,
            ChannelStats channelStats,
            List<String> categories,
            List<Video> videos,
            Map<Long, VideoStats> videoStatsMap
    ) {
        List<VideoMetric> metrics = buildVideoMetrics(videos, videoStatsMap);

        GetInfluencerInsightResponse.Audience audience = buildAudienceMetrics(metrics, channelStats);
        GetInfluencerInsightResponse.Content content = buildContentMetrics(metrics, channelStats);
        GetInfluencerInsightResponse.Activity activity = buildActivityMetrics(videos, channelStats);
        GetInfluencerInsightResponse.FormatAnalysis formatAnalysis = buildFormatAnalysis(metrics);

        return new GetInfluencerInsightResponse(
                channel.getId(),
                channel.getName(),
                channel.getChannelHandle(),
                channel.getYoutubePublishedAt(),
                channelStats != null ? channelStats.getSubscriberCount() : 0L,
                categories,
                new GetInfluencerInsightResponse.AiSummary(null),
                audience,
                content,
                activity,
                formatAnalysis
        );
    }

    private List<VideoMetric> buildVideoMetrics(List<Video> videos, Map<Long, VideoStats> videoStatsMap) {
        List<VideoMetric> metrics = new ArrayList<>();

        for (Video video : videos) {
            VideoStats stats = videoStatsMap.get(video.getId());
            if (stats == null) {
                continue;
            }

            long viewCount = stats.getViewCount();
            long likeCount = stats.getLikeCount();
            long commentCount = stats.getCommentCount();
            double engagementRate = AnalyticsCalculator.engagementRate(likeCount, commentCount, viewCount);
            double likeRate = calculateRatio(likeCount, viewCount);
            double commentRate = calculateRatio(commentCount, viewCount);
            double vph = stats.getVph() != null ? stats.getVph() : AnalyticsCalculator.vph(viewCount, video.getPublishedAt());
            double outlierScore = stats.getOutlierScore() != null ? stats.getOutlierScore() : 0.0;

            metrics.add(new VideoMetric(
                    video.isShort(),
                    video.getPublishedAt(),
                    viewCount,
                    likeCount,
                    commentCount,
                    engagementRate,
                    likeRate,
                    commentRate,
                    vph,
                    outlierScore
            ));
        }

        return metrics;
    }

    private GetInfluencerInsightResponse.Audience buildAudienceMetrics(
            List<VideoMetric> metrics,
            ChannelStats channelStats
    ) {
        long totalViews = 0L;
        long totalLikes = 0L;
        long totalComments = 0L;

        for (VideoMetric metric : metrics) {
            totalViews += metric.viewCount();
            totalLikes += metric.likeCount();
            totalComments += metric.commentCount();
        }

        double engagementRate = AnalyticsCalculator.engagementRate(totalLikes, totalComments, totalViews);

        double likeRate = calculateRatio(totalLikes, totalViews);
        double commentRate = calculateRatio(totalComments, totalViews);

        double viewsPerSubscriberRate = 0.0;
        if (channelStats != null && channelStats.getSubscriberCount() > 0) {
            double baselineViews = channelStats.getAvgViewsRecent() != null
                    ? channelStats.getAvgViewsRecent()
                    : averageViewCount(metrics);
            viewsPerSubscriberRate = calculateRatio(baselineViews, channelStats.getSubscriberCount());
        }

        return new GetInfluencerInsightResponse.Audience(
                calculateRound(engagementRate),
                calculateRound(likeRate),
                calculateRound(commentRate),
                calculateRound(viewsPerSubscriberRate)
        );
    }

    private GetInfluencerInsightResponse.Content buildContentMetrics(
            List<VideoMetric> metrics,
            ChannelStats channelStats
    ) {
        double viral2xRate = calculatePercentOf(metrics, metric -> metric.outlierScore() >= 2.0);
        double viral5xRate = calculatePercentOf(metrics, metric -> metric.outlierScore() >= 5.0);
        double medianVph = calculateMedian(metrics.stream()
                .map(VideoMetric::vph)
                .toList());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentStart = now.minusDays(RECENT_WINDOW_DAYS);
        LocalDateTime previousStart = now.minusDays(RECENT_WINDOW_DAYS * 2L);

        double recentAverageViews = channelStats != null && channelStats.getAvgViewsRecent() != null
                ? channelStats.getAvgViewsRecent()
                : averageViewCount(metrics.stream()
                .filter(metric -> metric.publishedAt() != null && !metric.publishedAt().isBefore(recentStart))
                .toList());

        double previousAverageViews = averageViewCount(metrics.stream()
                .filter(metric -> metric.publishedAt() != null
                        && metric.publishedAt().isBefore(recentStart)
                        && !metric.publishedAt().isBefore(previousStart))
                .toList());

        return new GetInfluencerInsightResponse.Content(
                calculateRound(viral2xRate),
                calculateRound(viral5xRate),
                calculateRound(medianVph),
                calculateRound(calculateGrowthRate(previousAverageViews, recentAverageViews))
        );
    }

    private GetInfluencerInsightResponse.Activity buildActivityMetrics(List<Video> videos, ChannelStats channelStats) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recent30d = now.minusDays(RECENT_WINDOW_DAYS);
        LocalDateTime previous30d = now.minusDays(RECENT_WINDOW_DAYS * 2L);

        long uploadsInRecent30d = channelStats != null && channelStats.getRecentUploadCount30d() != null
                ? channelStats.getRecentUploadCount30d()
                : videos.stream()
                .filter(video -> video.getPublishedAt() != null && !video.getPublishedAt().isBefore(recent30d))
                .count();

        long uploadsInPrevious30d = videos.stream()
                .filter(video -> video.getPublishedAt() != null
                        && video.getPublishedAt().isBefore(recent30d)
                        && !video.getPublishedAt().isBefore(previous30d))
                .count();

        double uploadsPerWeek = uploadsInRecent30d / (RECENT_WINDOW_DAYS / 7.0);

        return new GetInfluencerInsightResponse.Activity(
                calculateRound(uploadsPerWeek),
                resolveFrequencyTrend(uploadsInPrevious30d, uploadsInRecent30d)
        );
    }

    private GetInfluencerInsightResponse.FormatAnalysis buildFormatAnalysis(List<VideoMetric> metrics) {
        LocalDateTime recent30d = LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS);

        List<VideoMetric> recentLongForm = metrics.stream()
                .filter(metric -> metric.publishedAt() != null && !metric.publishedAt().isBefore(recent30d))
                .filter(metric -> !metric.isShort())
                .toList();

        List<VideoMetric> recentShortForm = metrics.stream()
                .filter(metric -> metric.publishedAt() != null && !metric.publishedAt().isBefore(recent30d))
                .filter(VideoMetric::isShort)
                .toList();

        return new GetInfluencerInsightResponse.FormatAnalysis(
                new GetInfluencerInsightResponse.FormatMetric(
                        calculateRound(averageViewCount(recentLongForm)),
                        calculateRound(averageEngagementRate(recentLongForm))
                ),
                new GetInfluencerInsightResponse.FormatMetric(
                        calculateRound(averageViewCount(recentShortForm)),
                        calculateRound(averageEngagementRate(recentShortForm))
                )
        );
    }

    private GetInfluencerInsightResponse.UploadFrequencyTrend resolveFrequencyTrend(long previousCount, long recentCount) {
        if (recentCount > previousCount) {
            return GetInfluencerInsightResponse.UploadFrequencyTrend.INCREASING;
        }
        if (recentCount < previousCount) {
            return GetInfluencerInsightResponse.UploadFrequencyTrend.DECREASING;
        }
        return GetInfluencerInsightResponse.UploadFrequencyTrend.STABLE;
    }

    private double calculatePercentOf(List<VideoMetric> metrics, Predicate<VideoMetric> predicate) {
        if (metrics.isEmpty()) {
            return 0.0;
        }

        long matched = metrics.stream()
                .filter(predicate)
                .count();

        return matched * 100.0 / metrics.size();
    }

    private double averageViewCount(List<VideoMetric> metrics) {
        if (metrics.isEmpty()) {
            return 0.0;
        }
        return metrics.stream()
                .mapToLong(VideoMetric::viewCount)
                .average()
                .orElse(0.0);
    }

    private double averageEngagementRate(List<VideoMetric> metrics) {
        if (metrics.isEmpty()) {
            return 0.0;
        }
        return metrics.stream()
                .mapToDouble(VideoMetric::engagementRate)
                .average()
                .orElse(0.0);
    }

    private double calculateGrowthRate(double previousValue, double recentValue) {
        if (previousValue <= 0.0) {
            return recentValue > 0.0 ? 100.0 : 0.0;
        }
        return ((recentValue - previousValue) / previousValue) * 100.0;
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = values.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    private double calculateRatio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator * 100.0 / denominator;
    }

    private double calculateRatio(double numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator * 100.0 / denominator;
    }

    private double calculateRound(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record VideoMetric(
            boolean isShort,
            LocalDateTime publishedAt,
            long viewCount,
            long likeCount,
            long commentCount,
            double engagementRate,
            double likeRate,
            double commentRate,
            double vph,
            double outlierScore
    ) {
    }
}
