package com.example.inflace.domain.brandcollaboration.dto.request;

import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.global.enums.SortOrder;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

public record ChannelBrandHistorySearchCondition(
        @Schema(description = "기간 필터 시작일 (RFC 3339)", example = "2024-01-01T00:00:00Z")
        String startDate,

        @Schema(description = "기간 필터 종료일 (RFC 3339)", example = "2024-12-31T23:59:59Z")
        String endDate,

        @Schema(description = "영상 형식", allowableValues = {"ALL", "LONG_FORM", "SHORT_FORM"}, defaultValue = "ALL")
        String videoFormat,

        @Schema(description = "영상 카테고리 ID", example = "26")
        String categoryId,

        @Schema(description = "정렬 기준", allowableValues = {"LATEST", "VIEW_COUNT"}, defaultValue = "LATEST")
        String sortCriteria,

        @Schema(description = "정렬 방향", allowableValues = {"DESC"}, defaultValue = "DESC")
        SortOrder sortOrder,

        @Schema(description = "이전 응답의 nextCursor 값. 다음 페이지 조회 시 그대로 전달합니다.", example = "TEFURVNUfERFU0N8dG9rZW4")
        String cursor,

        @Schema(description = "페이지 크기. 미입력 시 9", minimum = "1", defaultValue = "9", example = "9")
        Integer pageSize
) {
    private static final int DEFAULT_PAGE_SIZE = 9;
    private static final int MAX_PAGE_SIZE = 50;

    public ChannelBrandHistorySearchCondition {
        sortOrder = sortOrder == null ? SortOrder.DESC : sortOrder;
        if (SortOrder.ASC.equals(sortOrder)) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        cursor = StringUtils.hasText(cursor) ? cursor : null;
    }

    public String sortCriteriaValue() {
        return StringUtils.hasText(sortCriteria) ? sortCriteria.toUpperCase() : "LATEST";
    }

    public ChannelVideoFormat videoFormatEnum() {
        if (!StringUtils.hasText(videoFormat)) {
            return ChannelVideoFormat.ALL;
        }
        try {
            return ChannelVideoFormat.valueOf(videoFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChannelVideoFormat.ALL;
        }
    }

    public String youtubeOrder() {
        return "VIEW_COUNT".equals(sortCriteriaValue()) ? "viewCount" : "date";
    }
}
