package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.global.util.AnalyticsCalculator;

import java.util.ArrayList;
import java.util.List;

public record DropPointsResponse(List<DropPoint> dropPoints) {

    private static final int SEGMENT_SIZE = 25;
    private static final int SEGMENT_COUNT = 4;

    public record DropPoint(String startTime, String endTime, Double dropRate) {
    }

    public static DropPointsResponse from(List<AudienceRetention> retentionList, Double duration) {
        List<DropPoint> dropPoints = new ArrayList<>();

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            int from = i * SEGMENT_SIZE;
            int to = from + SEGMENT_SIZE;

            List<AudienceRetention> segment = retentionList.subList(from, to);

            List<Double> rates = segment.stream()
                    .map(AudienceRetention::getRetentionRate)
                    .toList();

            String startTime = AnalyticsCalculator.formatTime(segment.get(0).getTimeRatio(), duration);
            double dropRate = AnalyticsCalculator.avgChurnRate(rates);

            boolean isLastSegment = (i == SEGMENT_COUNT - 1);
            if (isLastSegment) {
                dropPoints.add(new DropPoint(startTime, null, dropRate));
            } else {
                String endTime = AnalyticsCalculator.formatTime(segment.get(SEGMENT_SIZE - 1).getTimeRatio(), duration);
                dropPoints.add(new DropPoint(startTime, endTime, dropRate));
            }
        }

        return new DropPointsResponse(dropPoints);
    }
}
