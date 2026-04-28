package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.enums.SortOrder;

import java.util.List;

public record InfluencerSearchCondition(
        String channelName,
        String uploadPeriod,
        String sortCriteria,
        List<String> categoryNames,
        Double engagementRateFrom,
        Double engagementRateTo,
        Long subscriberFrom,
        Long subscriberTo,
        String outlierRange,
        Double lastEngagementSortRate,
        Long lastSubscriberSortCount,
        Long lastChannelId,
        Integer pageSize,
        SortOrder sortOrder
) {
    private static final int DEFAULT_PAGE_SIZE = 9;

    public InfluencerSearchCondition {
        categoryNames = categoryNames == null ? List.of() : categoryNames;
        engagementRateFrom = engagementRateFrom == null ? 5.0 : engagementRateFrom;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        sortOrder = sortOrder == null ? SortOrder.DESC : sortOrder;

        if (uploadPeriod != null && !uploadPeriod.isBlank()) {
            InfluencerUploadPeriod.from(uploadPeriod);
        }
        if (sortCriteria != null && !sortCriteria.isBlank()) {
            InfluencerSortCriteria.from(sortCriteria);
        }
        if (outlierRange != null && !outlierRange.isBlank()) {
            InfluencerVideoOutlierRange.from(outlierRange);
        }
    }

    public InfluencerUploadPeriod uploadPeriodEnum() {
        return uploadPeriod == null || uploadPeriod.isBlank() ? null : InfluencerUploadPeriod.from(uploadPeriod);
    }

    public InfluencerSortCriteria sortCriteriaEnum() {
        return sortCriteria == null || sortCriteria.isBlank()
                ? InfluencerSortCriteria.ENGAGEMENT_RATE
                : InfluencerSortCriteria.from(sortCriteria);
    }

    public InfluencerVideoOutlierRange outlierRangeEnum() {
        return outlierRange == null || outlierRange.isBlank() ? null : InfluencerVideoOutlierRange.from(outlierRange);
    }

    public boolean hasSubscriberCursor() {
        return lastSubscriberSortCount != null && lastChannelId != null;
    }

    public boolean hasEngagementCursor() {
        return lastEngagementSortRate != null && lastChannelId != null;
    }
}
