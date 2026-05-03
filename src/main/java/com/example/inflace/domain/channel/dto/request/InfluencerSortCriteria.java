package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;

public enum InfluencerSortCriteria {
    SUBSCRIBER("subscriber"),
    ENGAGEMENT_RATE("engagement_rate");

    private final String value;

    InfluencerSortCriteria(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static InfluencerSortCriteria from(String value) {
        for (InfluencerSortCriteria sortCriteria : values()) {
            if (sortCriteria.value.equalsIgnoreCase(value)) {
                return sortCriteria;
            }
        }
        throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
    }
}
