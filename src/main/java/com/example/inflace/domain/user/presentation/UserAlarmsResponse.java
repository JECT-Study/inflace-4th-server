package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.domain.enums.AlarmType;

import java.util.List;

public record UserAlarmsResponse(
        String alarmEmail,
        List<AlarmInfo> alarms
) {
    public record AlarmInfo(
            AlarmType alarmType,
            boolean enabled
    ) {
    }
}
