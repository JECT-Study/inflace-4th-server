package com.example.inflace.domain.user.infra;

import java.util.UUID;

public record UserRegistrationResult(UUID userId, boolean isNewUser) {
}
