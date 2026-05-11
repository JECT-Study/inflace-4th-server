package com.example.inflace.domain.brandcollaboration.dto.response;

import java.util.List;

public record BrandCollaborationTrendsResponse(
        ContentKeywords contentKeywords,
        ChannelCharacteristics channelCharacteristics,
        StrategyInsight strategyInsight
) {
    // 콘텐츠 공통 키워드
    public record ContentKeywords(
            List<String> keywords,
            String keywordSummary
    ) {}

    // 채널 공통 특징
    // - channelCount, avgSubscribers, minSubscribers, maxSubscribers: channels.list statistics 직접 계산
    // - uploadIntervalDays: playlistItems.list 최근 10개 publishedAt 기반 평균 간격 (일 단위)
    // - categoryDistribution: 영상 snippet.categoryId → YoutubeCategory 테이블 룩업 후 비율 계산
    // TODO: avgViewPercentage(마성 시청 비율) — YouTube Analytics API averageViewPercentage (채널 오너 OAuth) 필요, 경쟁사 채널 접근 불가
    // TODO: audienceDemographics — YouTube Analytics API (채널 오너 OAuth) 필요, 경쟁사 채널 접근 불가
    public record ChannelCharacteristics(
            int channelCount,
            long avgSubscribers,
            long minSubscribers,
            long maxSubscribers,
            Double uploadIntervalDays,      // 평균 업로드 간격 (일), null = 데이터 부족
            List<CategoryShare> categoryDistribution
    ) {}

    public record CategoryShare(
            String category,
            int percentage
    ) {}

    // AI 전략 인사이트
    public record StrategyInsight(
            String pplIntent,
            String competitivePoints
    ) {}
}
