package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.global.util.AnalyticsCalculator;

import java.util.ArrayList;
import java.util.List;

public record AudienceRetentionResponse(
        List<RetentionData> retentionData
) {
    public record RetentionData(
            Double timeRatio,
            Double watchRatio,
            String displayTime,
            Boolean isDrop
    ) {
    }

    public static AudienceRetentionResponse from(List<AudienceRetention> retentionList, int durationSeconds) {
        List<RetentionData> data = new ArrayList<>();
        for (int i = 0; i < retentionList.size(); i++) {
            AudienceRetention r = retentionList.get(i);
            String displayTime = AnalyticsCalculator.formatTime(r.getTimeRatio(), durationSeconds);
            boolean isDrop = i > 0 &&
                    retentionList.get(i - 1).getRetentionRate() - r.getRetentionRate() >= 0.05;
            data.add(new RetentionData(r.getTimeRatio(), r.getRetentionRate(), displayTime, isDrop));
        }
        return new AudienceRetentionResponse(data);
    }
}
