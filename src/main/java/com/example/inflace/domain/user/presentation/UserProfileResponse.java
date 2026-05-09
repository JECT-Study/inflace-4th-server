package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        AccountInfo account,
        PreferenceInfo preferences
) {
    public record AccountInfo(
            String profileImageUrl,
            String name,
            String email,
            LocalDateTime enteredAt
    ) {
    }

    public record PreferenceInfo(
            List<UserRole> roles,
            List<Need> needs,
            List<UserRole> roleOptions,
            List<Need> needOptions
    ) {
    }
}
