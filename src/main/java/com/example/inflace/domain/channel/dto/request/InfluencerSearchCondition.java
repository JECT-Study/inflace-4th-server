package com.example.inflace.domain.channel.dto.request;

import com.example.inflace.global.enums.SortOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record InfluencerSearchCondition(
        @Schema(
                description = "채널명 부분 검색어",
                example = "침착맨"
        )
        String channelName,

        @Schema(
                description = "최근 업로드 주기 필터",
                allowableValues = {"7D", "30D", "31_90D", "91_180D", "180D_PLUS"},
                example = "30D"
        )
        String uploadPeriod,

        @Schema(
                description = "정렬 기준. 미입력 시 engagement_rate",
                allowableValues = {"subscriber", "engagement_rate"},
                defaultValue = "engagement_rate",
                example = "engagement_rate"
        )
        String sortCriteria,

        @ArraySchema(
                arraySchema = @Schema(description = "카테고리 필터. 동일한 query param을 반복 전달합니다."),
                schema = @Schema(example = "게임")
        )
        List<String> categoryNames,

        @Schema(
                description = "최소 참여율(%) 필터. 미입력 시 5.0",
                defaultValue = "5.0",
                example = "5.0"
        )
        Double engagementRateFrom,

        @Schema(
                description = "최대 참여율(%) 필터",
                example = "15.0"
        )
        Double engagementRateTo,

        @Schema(
                description = "최소 구독자 수 필터",
                example = "10000"
        )
        Long subscriberFrom,

        @Schema(
                description = "최대 구독자 수 필터",
                example = "300000"
        )
        Long subscriberTo,

        @Schema(
                description = "아웃라이어 구간 필터. channel_stats의 최근 아웃라이어 평균 기준",
                allowableValues = {"1.0X", "1.5X", "2.0X", "3.0X"},
                example = "1.5X"
        )
        String outlierRange,

        @Schema(
                description = "참여율 정렬 커서 값. sortCriteria=engagement_rate일 때 사용",
                example = "8.73"
        )
        Double lastEngagementSortRate,

        @Schema(
                description = "구독자 수 정렬 커서 값. sortCriteria=subscriber일 때 사용",
                example = "125000"
        )
        Long lastSubscriberSortCount,

        @Schema(
                description = "동일 정렬값 tie-break용 마지막 채널 ID. 커서 페이지네이션 시 함께 전달",
                example = "42"
        )
        Long lastChannelId,

        @Schema(
                description = "페이지 크기. 미입력 시 9",
                defaultValue = "9",
                example = "9"
        )
        Integer pageSize,

        @Schema(
                description = "정렬 방향. 미입력 시 DESC",
                allowableValues = {"ASC", "DESC"},
                defaultValue = "DESC",
                example = "DESC"
        )
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
