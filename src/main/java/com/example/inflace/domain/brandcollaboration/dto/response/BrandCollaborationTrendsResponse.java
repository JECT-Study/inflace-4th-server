package com.example.inflace.domain.brandcollaboration.dto.response;

import java.util.List;

public record BrandCollaborationTrendsResponse(
        List<String> commonKeywords,
        String channelInsight
) {
}
