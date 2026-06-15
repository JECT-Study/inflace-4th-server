package com.example.inflace.domain.channel.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InfluencerSearchConditionTest {

    @Test
    void defaultsMinimumEngagementRateToZero() {
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

        assertThat(condition.engagementRateFrom()).isZero();
    }
}
