package com.example.inflace.domain.brandcollaboration.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ChannelBrandHistoryAnalysisResponse(
        @Schema(description = "분석 대상 영상 수", example = "42")
        int videoCount,

        @Schema(description = "콘텐츠 유형별 평균 조회수 및 참여율")
        List<ContentTypeAvgViews> avgViewsByContentType,

        @Schema(description = "카테고리별 분포")
        List<CategoryShare> categoryDistribution,

        @Schema(description = "콘텐츠 유형별 분포 (롱폼/숏폼)")
        List<ContentTypeShare> contentTypeDistribution,

        // TODO: adScore — 점수 산정 기준 기획 필요
        @Schema(description = "광고 적합도 점수")
        AdScore adScore
) {
    public record ContentTypeShare(
            @Schema(description = "영상 형식", allowableValues = {"LONG_FORM", "SHORT_FORM"}, example = "LONG_FORM")
            String format,
            @Schema(description = "영상 수", example = "30")
            int count,
            @Schema(description = "비율 (%)", example = "71")
            int percentage
    ) {}

    public record CategoryShare(
            @Schema(description = "카테고리명", example = "뷰티/패션")
            String category,
            @Schema(description = "영상 수", example = "18")
            int count,
            @Schema(description = "비율 (%)", example = "45")
            int percentage
    ) {}

    public record ContentTypeAvgViews(
            @Schema(description = "영상 형식", allowableValues = {"LONG_FORM", "SHORT_FORM"}, example = "LONG_FORM")
            String format,
            @Schema(description = "평균 조회수", example = "324000")
            long avgViewCount,
            @Schema(description = "평균 참여율 (%)", example = "3.45")
            double avgEngagementRate
    ) {}

    public record AdScore(
            int score,
            String label,
            String viewStability,
            String subscriptionHealth,
            String collaborationExperience
    ) {}
}
