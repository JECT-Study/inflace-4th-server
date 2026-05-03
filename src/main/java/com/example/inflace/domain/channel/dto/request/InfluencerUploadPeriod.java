package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;

public enum InfluencerUploadPeriod {
    WITHIN_7_DAYS("7D", 0, 7),
    WITHIN_30_DAYS("30D", 0, 30),
    DAYS_31_TO_90("31_90D", 31, 90),
    DAYS_91_TO_180("91_180D", 91, 180),
    OVER_180_DAYS("180D_PLUS", 181, null);

    private final String value;
    private final Integer minDaysInclusive;
    private final Integer maxDaysInclusive;

    InfluencerUploadPeriod(String value, Integer minDaysInclusive, Integer maxDaysInclusive) {
        this.value = value;
        this.minDaysInclusive = minDaysInclusive;
        this.maxDaysInclusive = maxDaysInclusive;
    }

    public String value() {
        return value;
    }

    public Integer minDaysInclusive() {
        return minDaysInclusive;
    }

    public Integer maxDaysInclusive() {
        return maxDaysInclusive;
    }

    public static InfluencerUploadPeriod from(String value) {
        for (InfluencerUploadPeriod period : values()) {
            if (period.value.equalsIgnoreCase(value)) {
                return period;
            }
        }
        throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
    }

}
