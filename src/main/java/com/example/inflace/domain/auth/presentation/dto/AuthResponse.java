package com.example.inflace.domain.auth.presentation.dto;

public record AuthResponse(
        String AccessToken,
        String RefreshToken
) {
}
