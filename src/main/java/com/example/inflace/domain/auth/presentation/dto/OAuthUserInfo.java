package com.example.inflace.domain.auth.presentation.dto;

public record OAuthUserInfo(
        String sub,
        String name,
        String email,
        String picture
) {
}
