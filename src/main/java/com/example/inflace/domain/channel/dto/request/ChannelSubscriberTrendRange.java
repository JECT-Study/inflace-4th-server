package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.time.LocalDate;

public enum ChannelSubscriberTrendRange {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    DAYS_180("180D", 180),
    YEAR_1("1Y", 365);

    private final String value;
    private final int days;

    ChannelSubscriberTrendRange(String value, int days) {
        this.value = value;
        this.days = days;
    }

    public String value() {
        return value;
    }

    public LocalDate startDate(LocalDate endDate) {
        return endDate.minusDays(days - 1L);
    }

    public static ChannelSubscriberTrendRange from(String value) {
        for (ChannelSubscriberTrendRange range : values()) {
            if (range.value.equalsIgnoreCase(value)) {
                return range;
            }
        }
        throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
    }
}
