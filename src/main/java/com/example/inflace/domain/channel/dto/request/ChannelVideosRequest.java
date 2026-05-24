package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.time.LocalDate;

public record ChannelVideosRequest(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        ChannelVideoSort sort,
        ChannelVideoFormat format,
        Boolean isAd,
        String cursor,
        Integer size,
        Integer categoryId
) {
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    public ChannelVideosRequest {
        sort = sort == null ? ChannelVideoSort.LATEST : sort;
        format = format == null ? ChannelVideoFormat.ALL : format;
        size = normalizeSize(size);
        validateDateRange(startDate, endDate);
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(ErrorDefine.INVALID_DATE_RANGE);
        }
    }
}
