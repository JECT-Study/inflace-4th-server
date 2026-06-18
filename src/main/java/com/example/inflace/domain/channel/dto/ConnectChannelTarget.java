package com.example.inflace.domain.channel.dto;

import java.util.UUID;

public record ConnectChannelTarget(
        UUID userId,
        String googleId
) {
}
