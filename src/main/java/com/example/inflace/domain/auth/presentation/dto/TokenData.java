package com.example.inflace.domain.auth.presentation.dto;

public record TokenData(
        String accessToken,
        String refreshToken
) {
}
