package com.example.inflace.domain.brandcollaboration.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ChannelBrandHistoryAnalysisResponse(
        @Schema(description = "분석 대상 영상 수", example = "42")
        int videoCount,

        @Schema(description = "브랜드별 협업 영상 수 (재협업 카운팅 포함)")
        List<BrandCount> brands,

        @Schema(description = "콘텐츠 유형별 분포 (롱폼/숏폼)")
        List<ContentTypeShare> contentTypeDistribution,

        @Schema(description = "카테고리별 분포")
        List<CategoryShare> categoryDistribution,

        @Schema(description = "콘텐츠 유형별 평균 조회수")
        List<ContentTypeAvgViews> avgViewsByContentType

        // TODO: adScore — 계산 기준 기획 미정
        // TODO: subscriptionConversionRate — YouTube Analytics API (채널 오너 OAuth) 필요, 경쟁사 채널 접근 불가
        // TODO: commentPositiveRate — commentThreads.list API + AI 필요
) {
    public record BrandCount(
            @Schema(description = "브랜드명 (태그)", example = "APR")
            String brand,
            @Schema(description = "협업 영상 수", example = "5")
            int count
    ) {}

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
            @Schema(description = "비율 (%)", example = "45")
            int percentage
    ) {}

    public record ContentTypeAvgViews(
            @Schema(description = "영상 형식", allowableValues = {"LONG_FORM", "SHORT_FORM"}, example = "LONG_FORM")
            String format,
            @Schema(description = "평균 조회수", example = "324000")
            long avgViewCount
    ) {}
}
