package com.example.inflace.domain.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String provider,
        @NotBlank String code
) {
}
