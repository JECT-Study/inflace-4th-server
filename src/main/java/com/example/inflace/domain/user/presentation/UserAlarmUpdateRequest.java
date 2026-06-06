package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.AlarmType;
import jakarta.validation.constraints.NotNull;

public record UserAlarmUpdateRequest(
        @NotNull AlarmType alarmType,
        @NotNull Boolean enabled
) {
}
