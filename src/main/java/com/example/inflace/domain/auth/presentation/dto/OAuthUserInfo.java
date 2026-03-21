package com.example.inflace.domain.auth.presentation.dto;

public record OAuthUserInfo(
        String name,
        String email,
        String picture
) {
}
