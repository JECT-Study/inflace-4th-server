package com.example.inflace.domain.brand.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandTest {

    @Test
    void approvesBrandByAdmin() {
        Brand brand = new Brand();

        brand.approveByAdmin();

        assertThat(brand.isAdminApproved())
                .as("expected brand to be approved after admin approval")
                .isTrue();
    }
}
