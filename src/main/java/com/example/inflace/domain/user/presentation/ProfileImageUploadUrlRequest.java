package com.example.inflace.domain.user.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProfileImageUploadUrlRequest(
        @NotBlank String contentType,
        @Positive long fileSize
) {
}
