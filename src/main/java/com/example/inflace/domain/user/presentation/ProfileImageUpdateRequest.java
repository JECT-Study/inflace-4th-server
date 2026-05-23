package com.example.inflace.domain.user.presentation;

import jakarta.validation.constraints.NotBlank;

public record ProfileImageUpdateRequest(
        @NotBlank String objectKey
) {
}
