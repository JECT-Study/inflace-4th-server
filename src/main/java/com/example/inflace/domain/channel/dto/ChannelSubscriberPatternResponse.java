package com.example.inflace.domain.channel.dto;

public record ChannelSubscriberPatternResponse(
        NewSubscriber newSubscriber
) {
    public record NewSubscriber(
            Long count,
            Double ratio
    ) {
    }
    public static ChannelSubscriberPatternResponse from(Long totalViewCount, Long subscriberViewCount) {
        long totalViews = totalViewCount != null ? totalViewCount : 0L;
        long subscribedViews = subscriberViewCount != null ? subscriberViewCount : 0L;

        if (subscribedViews > totalViews) {
            subscribedViews = totalViews;
        }

        long newViews = Math.max(totalViews - subscribedViews, 0L);
        double newRatio = calculatePercentage(newViews, totalViews);

        return new ChannelSubscriberPatternResponse(
                new NewSubscriber(newViews, newRatio)
        );
    }

    private static double calculatePercentage(long value, long total) {
        if (total == 0L) {
            return 0.0;
        }
        return (value * 100.0) / total;
    }
}
