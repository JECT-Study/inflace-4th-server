package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OnboardingRequest(
        @NotEmpty List<UserRole> roles,
        @NotEmpty List<Need> needs
) {
}
