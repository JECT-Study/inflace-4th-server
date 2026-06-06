package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.AlarmType;

public record UserAlarmUpdateResponse(
        AlarmType alarmType,
        boolean enabled
) {
}
