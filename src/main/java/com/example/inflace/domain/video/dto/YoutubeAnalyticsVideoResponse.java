package com.example.inflace.domain.video.dto;

import java.util.List;

public record YoutubeAnalyticsVideoResponse(
        String kind,
        List<ColumnHeader> columnHeaders,
        List<List<Object>> rows   // 타입 혼재, 차후 service에서 타입 맞춰서 파싱
) {
    public record ColumnHeader(
            String name,
            String dataType,
            String columnType
    ) {}
}
