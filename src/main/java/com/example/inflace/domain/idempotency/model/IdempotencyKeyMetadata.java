package com.example.inflace.domain.idempotency.model;

import java.time.LocalDateTime;

public record IdempotencyKeyMetadata(
        String method,
        String path,
        LocalDateTime requestedAt
) {
}
