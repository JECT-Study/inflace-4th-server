package com.example.inflace.domain.channel.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InfluencerSearchConditionTest {

    @Test
    void defaultsSearchFiltersToRequestedInfluencerListValues() {
        InfluencerSearchCondition condition = new InfluencerSearchCondition(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(condition.categoryIds())
                .as("expected default categories to be travel/events, people/blogs, and howto/style")
                .containsExactly(7L, 10L, 14L);
        assertThat(condition.hasAdHistory())
                .as("expected default search to include only channels with advertisement history")
                .isTrue();
        assertThat(condition.engagementRateFrom())
                .as("expected default minimum engagement rate")
                .isEqualTo(2.0);
        assertThat(condition.engagementRateTo())
                .as("expected default maximum engagement rate")
                .isEqualTo(3.0);
    }
}
