package com.example.inflace.domain.brandcollaboration.dto.response;

import java.util.List;

public record BrandCollaborationTrendsResponse(
        // AI 분석
        List<String> commonKeywords,
        String keywordSummary,
        List<CategoryShare> categoryDistribution,
        StrategyInsight strategyInsight,

        // TODO: avgUploadFrequency, audienceDemographics
        ChannelStats channelStats
) {
    public record CategoryShare(
            String category,
            int percentage
    ) {
    }

    public record StrategyInsight(
            String pplIntent,
            String competitivePoints
    ) {
    }

    public record ChannelStats(
            int channelCount,
            String avgSubscribers,
            String subscriberRange
    ) {
    }
}
