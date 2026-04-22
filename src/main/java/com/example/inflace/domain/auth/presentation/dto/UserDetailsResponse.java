package com.example.inflace.domain.auth.presentation.dto;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.UserRole;

import java.util.List;
import java.util.UUID;

public record UserDetailsResponse(
        UUID id,
        String profileImage,
        List<UserRole> userRoles,
        Boolean isOnboardingCompleted
) {
    public static UserDetailsResponse of(User user, List<UserRole> userRoles) {
        return new UserDetailsResponse(
                user.getId(),
                user.getProfileImage(),
                userRoles,
                !userRoles.isEmpty()
        );
    }
}
