package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.AudienceRetention;

import java.util.List;

public record AudienceRetentionResponse(
        List<RetentionData> retentionData
) {
    public record RetentionData(
            Double timeRatio,
            Double watchRatio
    ) {
    }

    public static AudienceRetentionResponse from(List<AudienceRetention> retentionList) {
        List<RetentionData> data = retentionList.stream()
                .map(r -> new RetentionData(r.getTimeRatio(), r.getRetentionRate()))
                .toList();
        return new AudienceRetentionResponse(data);
    }
}
