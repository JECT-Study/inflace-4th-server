package com.example.inflace.domain.auth.presentation.dto;

import com.example.inflace.domain.user.domain.enums.Plan;

public record OAuthUserInfo(
        String sub,
        String name,
        String email,
        String picture,
        Plan plan,
        String accessToken
) {
}
