package com.example.inflace.domain.user.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserAlarmEmailUpdateRequest(
        @NotBlank @Email String alarmEmail
) {
}
