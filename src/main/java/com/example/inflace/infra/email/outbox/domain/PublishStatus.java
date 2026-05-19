package com.example.inflace.infra.email.outbox.domain;

public enum PublishStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
