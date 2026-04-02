package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.UserRole;

import java.util.List;

public record OnboardingRequest(
        UserRole role,
        List<Need> need
) {
}
