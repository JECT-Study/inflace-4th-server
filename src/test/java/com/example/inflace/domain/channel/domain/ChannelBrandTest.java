package com.example.inflace.domain.channel.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelBrandTest {

    @Test
    void approvesChannelBrandByAdmin() {
        ChannelBrand channelBrand = new ChannelBrand();

        channelBrand.approveByAdmin();

        assertThat(channelBrand.isAdminApproved())
                .as("expected channel brand to be approved after admin approval")
                .isTrue();
    }
}
