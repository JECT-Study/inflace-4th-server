package com.example.inflace.domain.auth.presentation.dto;

public record GoogleUserInfoResponse(
        String sub,
        String name,
        String email,
        String picture
) {
}
