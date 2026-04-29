package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;

public enum InfluencerVideoOutlierRange {
    FROM_1_0X("1.0X", 1.0),
    FROM_1_5X("1.5X", 1.5),
    FROM_2_0X("2.0X", 2.0),
    FROM_3_0X("3.0X", 3.0);

    private final String value;
    private final Double minValueInclusive;

    InfluencerVideoOutlierRange(String value, Double minValueInclusive) {
        this.value = value;
        this.minValueInclusive = minValueInclusive;
    }

    public String value() {
        return value;
    }

    public Double minValueInclusive() {
        return minValueInclusive;
    }

    public boolean contains(Double outlierScore) {
        if (outlierScore == null) {
            return false;
        }

        return outlierScore >= minValueInclusive;
    }

    public static InfluencerVideoOutlierRange from(String value) {
        for (InfluencerVideoOutlierRange range : values()) {
            if (range.value.equalsIgnoreCase(value)) {
                return range;
            }
        }
        throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
    }
}
