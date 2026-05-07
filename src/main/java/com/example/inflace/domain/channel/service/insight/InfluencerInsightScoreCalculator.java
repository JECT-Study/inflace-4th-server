package com.example.inflace.domain.channel.service.insight;

import org.springframework.stereotype.Component;

@Component
public class InfluencerInsightScoreCalculator {

    private static final double AUDIENCE_ENGAGEMENT_WEIGHT = 35.0;
    private static final double AUDIENCE_LIKE_WEIGHT = 25.0;
    private static final double AUDIENCE_COMMENT_WEIGHT = 15.0;
    private static final double AUDIENCE_VIEWS_PER_SUBSCRIBER_WEIGHT = 25.0;
    private static final double CONTENT_VIRAL_2X_WEIGHT = 30.0;
    private static final double CONTENT_VIRAL_5X_WEIGHT = 20.0;
    private static final double CONTENT_MEDIAN_VPH_WEIGHT = 25.0;
    private static final double CONTENT_GROWTH_WEIGHT = 25.0;
    private static final double ACTIVITY_UPLOAD_CYCLE_WEIGHT = 35.0;
    private static final double ACTIVITY_FREQUENCY_CHANGE_WEIGHT = 25.0;
    private static final double AD_VIEW_STABILITY_WEIGHT = 30.0;
    private static final double AD_SUBSCRIBER_HEALTH_WEIGHT = 30.0;
    private static final double AD_SPONSORSHIP_EXPERIENCE_WEIGHT = 20.0;
    private static final double ACTIVITY_TOTAL_WEIGHT =
            ACTIVITY_UPLOAD_CYCLE_WEIGHT + ACTIVITY_FREQUENCY_CHANGE_WEIGHT;
    private static final double ADVERTISEMENT_TOTAL_WEIGHT =
            AD_VIEW_STABILITY_WEIGHT + AD_SUBSCRIBER_HEALTH_WEIGHT + AD_SPONSORSHIP_EXPERIENCE_WEIGHT;

    public double audienceScore(
            double engagementRate,
            double likeRate,
            double commentRate,
            double viewsPerSubscriberRate
    ) {
        return divideBy100(
                thresholdScore(engagementRate, 6.0) * AUDIENCE_ENGAGEMENT_WEIGHT
                        + thresholdScore(likeRate, 4.0) * AUDIENCE_LIKE_WEIGHT
                        + thresholdScore(commentRate, 0.5) * AUDIENCE_COMMENT_WEIGHT
                        + thresholdScore(viewsPerSubscriberRate, 20.0) * AUDIENCE_VIEWS_PER_SUBSCRIBER_WEIGHT
        );
    }

    public double contentScore(
            double viral2xRate,
            double viral5xRate,
            double medianVph,
            double growthTrendRate
    ) {
        return divideBy100(
                thresholdScore(viral2xRate, 30.0) * CONTENT_VIRAL_2X_WEIGHT
                        + thresholdScore(viral5xRate, 10.0) * CONTENT_VIRAL_5X_WEIGHT
                        + thresholdScore(medianVph, 500.0) * CONTENT_MEDIAN_VPH_WEIGHT
                        + thresholdScore(growthTrendRate, 30.0) * CONTENT_GROWTH_WEIGHT
        );
    }

    public double activityScore(double averageIntervalDays, double intervalChange) {
        double uploadCycleScore = averageIntervalDays <= 0.0
                ? 0.0
                : thresholdScore(14.0, averageIntervalDays);
        double frequencyChangeScore = clamp(50.0 - intervalChange);

        return calculateWeightedAverage(
                ACTIVITY_TOTAL_WEIGHT,
                uploadCycleScore * ACTIVITY_UPLOAD_CYCLE_WEIGHT
                        + frequencyChangeScore * ACTIVITY_FREQUENCY_CHANGE_WEIGHT
        );
    }

    public double advertisementScore(
            double viewStabilityScore,
            double subscriberHealthScore,
            double sponsorshipExperienceScore
    ) {
        return calculateWeightedAverage(
                ADVERTISEMENT_TOTAL_WEIGHT,
                viewStabilityScore * AD_VIEW_STABILITY_WEIGHT
                        + subscriberHealthScore * AD_SUBSCRIBER_HEALTH_WEIGHT
                        + sponsorshipExperienceScore * AD_SPONSORSHIP_EXPERIENCE_WEIGHT
        );
    }

    public double viewStabilityScore(double coefficientOfVariation) {
        return rangedPeakScore(coefficientOfVariation, 0.4, 1.8);
    }

    public double subscriberHealthScore(double subscriberHealthRate) {
        return rangedPeakScore(subscriberHealthRate, 2.0, 50.0);
    }

    public double sponsorshipExperienceScore(double sponsorshipExperienceRate) {
        return thresholdScore(sponsorshipExperienceRate, 20.0);
    }

    private double divideBy100(double weightedSum) {
        return clamp(weightedSum / 100.0);
    }

    private double calculateWeightedAverage(double totalWeight, double weightedSum) {
        if (totalWeight <= 0.0) {
            return 0.0;
        }
        return clamp(weightedSum / totalWeight);
    }

    private double thresholdScore(double actualValue, double fullScoreThreshold) {
        if (fullScoreThreshold <= 0.0) {
            return 0.0;
        }
        return clamp((actualValue / fullScoreThreshold) * 100.0);
    }

    private double rangedPeakScore(double actualValue, double lowerBound, double upperBound) {
        if (lowerBound < 0.0 || upperBound <= lowerBound) {
            return 0.0;
        }
        if (actualValue >= lowerBound && actualValue <= upperBound) {
            return 100.0;
        }
        if (actualValue < lowerBound) {
            return clamp((actualValue / lowerBound) * 100.0);
        }

        double upperRange = upperBound - lowerBound;
        if (upperRange <= 0.0) {
            return 0.0;
        }

        double overflow = actualValue - upperBound;
        return clamp(100.0 - (overflow / upperRange) * 100.0);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(value, 100.0));
    }
}
