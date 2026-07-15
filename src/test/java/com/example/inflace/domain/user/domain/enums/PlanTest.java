package com.example.inflace.domain.user.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    @Test
    void includesAdminPlan() {
        assertThat(Plan.valueOf("ADMIN"))
                .as("expected ADMIN to be available as a user plan")
                .isEqualTo(Plan.ADMIN);
    }
}
