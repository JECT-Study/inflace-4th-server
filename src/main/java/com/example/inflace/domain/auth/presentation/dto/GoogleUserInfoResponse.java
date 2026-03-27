package com.example.inflace.domain.auth.presentation.dto;

public record GoogleUserInfoResponse(
        String name,
        String email,
        String picture
) {
}
