package com.example.inflace.domain.brandcollaboration.dto.request;

import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.global.enums.SortOrder;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.util.StringUtils;

public record BrandCollaborationSearchCondition(
        @Schema(description = "검색할 브랜드명", example = "아모레퍼시픽")
        String brandName,

        @Schema(description = "기간 필터 시작일 (RFC 3339)", example = "2024-01-01T00:00:00Z")
        String startDate,

        @Schema(description = "기간 필터 종료일 (RFC 3339)", example = "2024-12-31T23:59:59Z")
        String endDate,

        @ArraySchema(schema = @Schema(example = "립밤"))
        List<String> includeKeywords,

        @ArraySchema(schema = @Schema(example = "쿠팡파트너스"))
        List<String> excludeKeywords,

        @Schema(description = "영상 형식", allowableValues = {"ALL", "LONG_FORM", "SHORT_FORM"}, defaultValue = "ALL")
        String videoFormat,

        @Schema(description = "영상 카테고리 ID", example = "26")
        String categoryId,

        @Schema(description = "지역 코드 (ISO 3166-1 alpha-2)", example = "KR")
        String regionCode,

        @Schema(description = "언어 코드 (BCP-47)", example = "ko")
        String languageCode,

        @Schema(description = "최소 조회수 필터", example = "1000", defaultValue = "0")
        Long minViews,

        @Schema(description = "최소 좋아요수 필터", example = "100", defaultValue = "0")
        Long minLikes,

        @Schema(description = "최소 댓글수 필터", example = "10", defaultValue = "0")
        Long minComments,

        @Schema(description = "정렬 기준", allowableValues = {"LATEST", "VIEW_COUNT", "ENGAGEMENT"}, defaultValue = "LATEST")
        String sortCriteria,

        @Schema(description = "정렬 방향", allowableValues = {"ASC", "DESC"}, defaultValue = "DESC")
        SortOrder sortOrder,

        @Schema(description = "이전 응답의 nextCursor 값. 다음 페이지 조회 시 그대로 전달합니다.", example = "TEFURVNUfERFU0N8dG9rZW4")
        String cursor,

        @Schema(description = "페이지 크기. 미입력 시 9", minimum = "1", defaultValue = "9", example = "9")
        Integer pageSize
) {
    private static final int DEFAULT_PAGE_SIZE = 9;

    public BrandCollaborationSearchCondition {
        if (!StringUtils.hasText(brandName)) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
        includeKeywords = includeKeywords == null ? List.of() : includeKeywords;
        excludeKeywords = excludeKeywords == null ? List.of() : excludeKeywords;
        minViews = minViews == null ? 0L : minViews;
        minLikes = minLikes == null ? 0L : minLikes;
        minComments = minComments == null ? 0L : minComments;
        sortOrder = sortOrder == null ? SortOrder.DESC : sortOrder;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        cursor = StringUtils.hasText(cursor) ? cursor : null;

        if (pageSize < 1) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
        if (excludeKeywords.size() > 5) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
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
        return switch (sortCriteriaValue()) {
            case "VIEW_COUNT" -> "viewCount";
            case "ENGAGEMENT" -> "rating";
            default -> "date";
        };
    }
}
