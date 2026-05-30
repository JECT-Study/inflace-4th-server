package com.example.inflace.domain.channel.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record GetInfluencerInsightResponse(
        @Schema(description = "채널 ID", example = "42")
        Long channelId,

        @Schema(description = "채널명", example = "침착맨")
        String channelName,

        @Schema(description = "채널 핸들", example = "@chimchakman")
        String channelHandle,

        @Schema(description = "채널 프로필 이미지 URL", example = "https://yt3.ggpht.com/oQnwIVz1_jEh84oyAJP4VmqyAOu0BqFjqq1q5LwRzE2NqchtoH4diA6Y6YYSFnJ7nJawYdHd=s800-c-k-c0x00ffffff-no-rj")
        String profileImageUrl,

        @Schema(description = "채널 배너 이미지 URL", example = "https://yt3.ggpht.com/oQnwIVz1_jEh84oyAJP4VmqyAOu0BqFjqq1q5LwRzE2NqchtoH4diA6Y6YYSFnJ7nJawYdHd=s800-c-k-c0x00ffffff-no-rj")
        String bannerImageUrl,

        @JsonFormat(pattern = "yyyy.MM.dd")
        @Schema(description = "채널 가입일", example = "2020.01.01")
        LocalDateTime joinedAt,

        @Schema(description = "총 구독자 수", example = "125000")
        Long subscriberCount,

        @Schema(description = "현재 로그인 유저의 즐겨찾기 여부", example = "true")
        boolean bookmarked,

        @ArraySchema(
                arraySchema = @Schema(description = "채널 카테고리 목록"),
                schema = @Schema(example = "게임")
        )
        List<String> categories,

        @Schema(description = "팬층 지표")
        Audience audience,

        @Schema(description = "콘텐츠 지표")
        Content content,

        @Schema(description = "활동 지표")
        Activity activity,

        @Schema(description = "광고 지표")
        Advertisement advertisement,

        @Schema(description = "롱폼 vs 숏폼 분석")
        FormatAnalysis formatAnalysis
) {
    public GetInfluencerInsightResponse withBookmarked(boolean bookmarked) {
        return new GetInfluencerInsightResponse(
                channelId,
                channelName,
                channelHandle,
                profileImageUrl,
                bannerImageUrl,
                joinedAt,
                subscriberCount,
                bookmarked,
                categories,
                audience,
                content,
                activity,
                advertisement,
                formatAnalysis
        );
    }

    public record Audience(
            @Schema(description = "팬층 종합 점수", example = "84.5")
            double score,

            @Schema(description = "채널 전체 참여율(%)", example = "7.2")
            double engagementRate,

            @Schema(description = "채널 전체 좋아요 비율(%)", example = "2.1")
            double likeRate,

            @Schema(description = "채널 전체 댓글 비율(%)", example = "1.5")
            double commentRate,

            @Schema(description = "구독자 대비 평균 조회 비율(%)", example = "22.0")
            double viewsPerSubscriberRate
    ) {
    }

    public record Content(
            @Schema(description = "콘텐츠 종합 점수", example = "76.3")
            double score,

            @Schema(description = "2배 이상 바이럴 영상 비율(%)", example = "38.0")
            double viral2xRate,

            @Schema(description = "5배 이상 바이럴 영상 비율(%)", example = "12.0")
            double viral5xRate,

            @Schema(description = "VPH 중앙값", example = "720.0")
            double medianVph,

            @Schema(description = "직전 30일 대비 최근 30일 평균 조회수 성장률(%)", example = "28.0")
            double growthTrendRate
    ) {
    }

    public record Activity(
            @Schema(description = "활동 종합 점수", example = "68.4")
            double score,

            @Schema(description = "최근 업로드 후 경과 일수", example = "2")
            int recentUpload,

            @Schema(description = "주간 업로드 횟수", example = "1.4")
            double uploadCycle,

            @Schema(description = "업로드 빈도 변화", example = "INCREASING")
            UploadFrequencyTrend frequencyTrend
    ) {
    }

    public record Advertisement(
            @Schema(description = "광고 종합 점수", example = "72.8")
            double score,

            @Schema(description = "조회수 변동계수(CV)", example = "0.92")
            double viewCoefficientOfVariation,

            @Schema(description = "구독 건강도 지표(구독자 대비 평균 조회 비율, %)", example = "22.0")
            double subscriberHealthRate
    ) {
    }

    public enum UploadFrequencyTrend {
        INCREASING,
        DECREASING,
        STABLE
    }

    public record FormatAnalysis(
            @Schema(description = "롱폼 지표")
            FormatMetric longForm,

            @Schema(description = "숏폼 지표")
            FormatMetric shortForm
    ) {
    }

    public record FormatMetric(
            @Schema(description = "최근 30일 영상 개수", example = "12")
            int count,

            @Schema(description = "최근 30일 평균 조회수", example = "185432.0")
            double averageViews30d,

            @Schema(description = "최근 30일 참여율(%)", example = "8.7")
            double engagementRate
    ) {
    }
}
