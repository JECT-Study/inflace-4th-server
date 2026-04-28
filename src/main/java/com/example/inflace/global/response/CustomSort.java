package com.example.inflace.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CustomSort(
        @Schema(description = "정렬 적용 여부", example = "true")
        boolean sorted,

        @Schema(description = "실제 적용된 정렬 기준", example = "engagement_rate")
        String sortCriteria,

        @Schema(description = "실제 적용된 정렬 방향", example = "DESC")
        String sortOrder
) {
    public static CustomSort of(boolean sorted, String sortCriteria, String sortOrder) {
        return new CustomSort(
                sorted,
                sortCriteria,
                sortOrder
        );
    }
}
