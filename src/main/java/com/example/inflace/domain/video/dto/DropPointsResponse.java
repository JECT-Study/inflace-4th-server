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

    public static DropPointsResponse from(List<AudienceRetention> retentionList, int durationSeconds) {
        List<DropPoint> dropPoints = new ArrayList<>();

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            List<AudienceRetention> segment = retentionList.subList(i * SEGMENT_SIZE, (i + 1) * SEGMENT_SIZE);

            String startTime = calcTime(segment.get(0), durationSeconds);
            double dropRate = calcDropRate(segment);

            boolean isLastSegment = (i == SEGMENT_COUNT - 1);
            if (isLastSegment) {
                dropPoints.add(new DropPoint(startTime, null, dropRate));
            } else {
                String endTime = calcTime(segment.get(SEGMENT_SIZE - 1), durationSeconds);
                dropPoints.add(new DropPoint(startTime, endTime, dropRate));
            }
        }

        return new DropPointsResponse(dropPoints);
    }

    private static double calcDropRate(List<AudienceRetention> segment) {
        List<Double> rates = segment.stream()
                .map(AudienceRetention::getRetentionRate)
                .toList();
        return AnalyticsCalculator.avgChurnRate(rates);
    }

    private static String calcTime(AudienceRetention point, int durationSeconds) {
        return AnalyticsCalculator.formatTime(point.getTimeRatio(), durationSeconds);
    }
}
