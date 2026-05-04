package com.example.inflace.domain.channel.service.insight;

import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;

import java.util.List;

public record InfluencerInsightQueryResult(
        String channelDescription,
        List<String> recentVideoDescriptions,
        GetInfluencerInsightResponse insight
) {
}
