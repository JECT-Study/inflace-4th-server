package com.example.inflace.domain.channel.service.insight;

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

import static java.time.Duration.between;

@Component
public class InfluencerInsightCalculator {

    private final InfluencerInsightScoreCalculator scoreCalculator;

    private static final int CONTENT_WINDOW_SIZE = 50;
    private static final int GROWTH_WINDOW_SIZE = 25;
    private static final int FREQUENCY_INTERVAL_WINDOW_SIZE = 5;
    private static final int RECENT_WINDOW_DAYS = 30;

    public InfluencerInsightCalculator(InfluencerInsightScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    public GetInfluencerInsightResponse calculate(
            Channel channel,
            ChannelStats channelStats,
            List<String> categories,
            List<Video> videos,
            Map<Long, VideoStats> videoStatsMap
    ) {
        List<VideoMetric> metrics = buildVideoMetrics(videos, videoStatsMap);
        List<Video> latestVideos = getLatestVideos(videos);
        List<VideoMetric> latestMetrics = getLatestMetrics(metrics);

        GetInfluencerInsightResponse.Audience audience = buildAudienceMetrics(latestMetrics, channelStats);
        GetInfluencerInsightResponse.Content content = buildContentMetrics(latestMetrics);
        GetInfluencerInsightResponse.Activity activity = buildActivityMetrics(latestVideos);
        GetInfluencerInsightResponse.Advertisement advertisement = buildAdvertisementMetrics(latestMetrics, channelStats);
        GetInfluencerInsightResponse.FormatAnalysis formatAnalysis = buildFormatAnalysis(metrics);

        return new GetInfluencerInsightResponse(
                channel.getId(),
                channel.getName(),
                channel.getChannelHandle(),
                channel.getProfileImageUrl(),
                channel.getBannerImageUrl(),
                channel.getYoutubePublishedAt(),
                channelStats != null ? channelStats.getSubscriberCount() : 0L,
                categories,
                audience,
                content,
                activity,
                advertisement,
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
                    video.isAdvertisement(),
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
            viewsPerSubscriberRate = calculateRatio(averageViewCount(metrics), channelStats.getSubscriberCount());
        }
        double score = scoreCalculator.audienceScore(
                engagementRate,
                likeRate,
                commentRate,
                viewsPerSubscriberRate
        );

        return new GetInfluencerInsightResponse.Audience(
                calculateRound(score),
                calculateRound(engagementRate),
                calculateRound(likeRate),
                calculateRound(commentRate),
                calculateRound(viewsPerSubscriberRate)
        );
    }

    private GetInfluencerInsightResponse.Content buildContentMetrics(
            List<VideoMetric> metrics
    ) {
        double averageViews = averageViewCount(metrics);

        double viral2xRate = calculateFixedWindowPercentOf(metrics, metric -> metric.viewCount() > averageViews * 2);
        double viral5xRate = calculateFixedWindowPercentOf(metrics, metric -> metric.viewCount() > averageViews * 5);
        double medianVph = calculateMedian(metrics.stream()
                .map(VideoMetric::vph)
                .toList());

        List<VideoMetric> recent25 = metrics.stream()
                .limit(GROWTH_WINDOW_SIZE)
                .toList();
        List<VideoMetric> previous25 = metrics.stream()
                .skip(GROWTH_WINDOW_SIZE)
                .limit(GROWTH_WINDOW_SIZE)
                .toList();
        double growthTrendRate = calculateGrowthRate(
                averageViewCount(previous25),
                averageViewCount(recent25)
        );
        double score = scoreCalculator.contentScore(
                viral2xRate,
                viral5xRate,
                medianVph,
                growthTrendRate
        );

        return new GetInfluencerInsightResponse.Content(
                calculateRound(score),
                calculateRound(viral2xRate),
                calculateRound(viral5xRate),
                calculateRound(medianVph),
                calculateRound(growthTrendRate)
        );
    }

    private GetInfluencerInsightResponse.Activity buildActivityMetrics(List<Video> videos) {
        double averageIntervalDays = averageIntervalDays(videos);
        double recentAverageIntervalDays = averageIntervalDays(videos, 0, FREQUENCY_INTERVAL_WINDOW_SIZE);
        double previousAverageIntervalDays = averageIntervalDays(
                videos,
                FREQUENCY_INTERVAL_WINDOW_SIZE,
                FREQUENCY_INTERVAL_WINDOW_SIZE
        );
        double intervalChange = recentAverageIntervalDays - previousAverageIntervalDays;

        double uploadsPerWeek = averageIntervalDays <= 0.0
                ? 0.0
                : 7.0 / averageIntervalDays;
        double score = scoreCalculator.activityScore(averageIntervalDays, intervalChange);

        return new GetInfluencerInsightResponse.Activity(
                calculateRound(score),
                calculateRecentUploadDays(videos),
                calculateRound(uploadsPerWeek),
                resolveFrequencyTrend(intervalChange, recentAverageIntervalDays, previousAverageIntervalDays)
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

    private GetInfluencerInsightResponse.Advertisement buildAdvertisementMetrics(
            List<VideoMetric> metrics,
            ChannelStats channelStats
    ) {
        double averageViews = averageViewCount(metrics);
        double viewCoefficientOfVariation = calculateCoefficientOfVariation(metrics, averageViews);
        double subscriberHealthRate = 0.0;
        if (channelStats != null && channelStats.getSubscriberCount() > 0) {
            subscriberHealthRate = calculateRatio(averageViews, channelStats.getSubscriberCount());
        }
        double sponsorshipExperienceRate = calculateFixedWindowPercentOf(metrics, VideoMetric::isAdvertisement);

        double viewStabilityScore = scoreCalculator.viewStabilityScore(viewCoefficientOfVariation);
        double subscriberHealthScore = scoreCalculator.subscriberHealthScore(subscriberHealthRate);
        double sponsorshipExperienceScore = scoreCalculator.sponsorshipExperienceScore(sponsorshipExperienceRate);
        double score = scoreCalculator.advertisementScore(
                viewStabilityScore,
                subscriberHealthScore,
                sponsorshipExperienceScore
        );

        return new GetInfluencerInsightResponse.Advertisement(
                calculateRound(score),
                calculateRound(viewCoefficientOfVariation),
                calculateRound(subscriberHealthRate)
        );
    }

    private record VideoMetric(
            boolean isShort,
            boolean isAdvertisement,
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

    private double calculateFixedWindowPercentOf(
            List<VideoMetric> metrics,
            Predicate<VideoMetric> predicate
    ) {
        if (metrics.isEmpty()) {
            return 0.0;
        }

        long matched = metrics.stream()
                .filter(predicate)
                .count();

        return matched * 100.0 / CONTENT_WINDOW_SIZE;
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

    private double calculateCoefficientOfVariation(List<VideoMetric> metrics, double averageViews) {
        if (metrics.isEmpty() || averageViews <= 0.0) {
            return 0.0;
        }

        double variance = metrics.stream()
                .mapToDouble(metric -> Math.pow(metric.viewCount() - averageViews, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance) / averageViews;
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

    private List<VideoMetric> getLatestMetrics(List<VideoMetric> metrics) {
        return metrics.stream()
                .filter(metric -> metric.publishedAt() != null)
                .limit(CONTENT_WINDOW_SIZE)
                .toList();
    }

    private List<Video> getLatestVideos(List<Video> videos) {
        return videos.stream()
                .filter(video -> video.getPublishedAt() != null)
                .limit(CONTENT_WINDOW_SIZE)
                .toList();
    }

    private double averageIntervalDays(List<Video> videos) {
        return averageIntervalDays(videos, 0, Integer.MAX_VALUE);
    }

    private double averageIntervalDays(List<Video> videos, int startIntervalIndex, int intervalCount) {
        if (videos.size() < startIntervalIndex + 2 || intervalCount <= 0) {
            return 0.0;
        }

        double sum = 0.0;
        int counted = 0;

        for (int i = startIntervalIndex; i < videos.size() - 1 && counted < intervalCount; i++) {
            LocalDateTime current = videos.get(i).getPublishedAt();
            LocalDateTime next = videos.get(i + 1).getPublishedAt();
            if (current == null || next == null) {
                continue;
            }

            double intervalDays = Math.abs(between(next, current).toMinutes()) / 1440.0;
            sum += intervalDays;
            counted++;
        }

        if (counted == 0) {
            return 0.0;
        }
        return sum / counted;
    }

    private int calculateRecentUploadDays(List<Video> videos) {
        if (videos.isEmpty() || videos.getFirst().getPublishedAt() == null) {
            return -1;
        }

        LocalDateTime latestPublishedAt = videos.getFirst().getPublishedAt();
        return (int) Math.max(0, between(latestPublishedAt, LocalDateTime.now()).toDays());
    }

    private GetInfluencerInsightResponse.UploadFrequencyTrend resolveFrequencyTrend(
            double intervalChange,
            double recentAverageIntervalDays,
            double previousAverageIntervalDays
    ) {
        if (recentAverageIntervalDays <= 0.0 || previousAverageIntervalDays <= 0.0) {
            return GetInfluencerInsightResponse.UploadFrequencyTrend.STABLE;
        }
        if (intervalChange < 0.0) {
            return GetInfluencerInsightResponse.UploadFrequencyTrend.INCREASING;
        }
        if (intervalChange > 0.0) {
            return GetInfluencerInsightResponse.UploadFrequencyTrend.DECREASING;
        }
        return GetInfluencerInsightResponse.UploadFrequencyTrend.STABLE;
    }

}
