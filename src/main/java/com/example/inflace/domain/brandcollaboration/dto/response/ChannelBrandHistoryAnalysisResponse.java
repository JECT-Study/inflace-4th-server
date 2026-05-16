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
            @Schema(description = "광고 적합도 종합 점수 (0~100)", example = "87")
            int score,
            @Schema(description = "종합 점수 등급", example = "높음")
            String label,
            @Schema(description = "조회수 안정성 등급", example = "높음")
            String viewStability,
            @Schema(description = "조회수 변동계수 (낮을수록 안정적)", example = "0.82")
            double viewStabilityCv,
            @Schema(description = "구독 건강도 등급", example = "높음")
            String subscriptionHealth,
            @Schema(description = "구독자 대비 평균 조회 비율 (%)", example = "15.2")
            double subscriptionHealthRate,
            @Schema(description = "협찬 경험도 등급", example = "높음")
            String collaborationExperience,
            @Schema(description = "전체 영상 중 PPL 영상 비율 (%)", example = "22.0")
            double collaborationRate
    ) {}
}
