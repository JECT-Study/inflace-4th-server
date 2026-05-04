package com.example.inflace.domain.video.dto;

import java.time.LocalDate;
import java.util.List;

public record YoutubeAnalyticsVideoRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<String> metrics,
        String youtubeVideoId,
        String dimensions,
        String filters
) {
    public String formattedMetricsList() {
        return String.join(",", metrics);
    }
}
