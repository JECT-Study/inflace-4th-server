package com.example.inflace.domain.channel.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record InfluencerSearchResponse(
        @Schema(description = "채널 ID", example = "42")
        Long channelId,

        @Schema(description = "채널명", example = "침착맨")
        String channelName,

        @Schema(description = "채널 핸들", example = "@chimchakman")
        String channelHandle,

        @Schema(description = "채널 썸네일 URL", example = "https://yt3.ggpht.com/example=s176-c-k-c0x00ffffff-no-rj")
        String thumbnailUrl,

        @ArraySchema(
                arraySchema = @Schema(description = "채널 카테고리 목록"),
                schema = @Schema(example = "게임")
        )
        List<String> categories,

        @Schema(description = "구독자 수", example = "125000")
        Long subscriberCount,

        @Schema(description = "최근 평균 참여율(%)", example = "8.73")
        Double averageEngagementRate,

        @Schema(description = "최근 평균 조회수", example = "185432.0")
        Double averageViews,

        @Schema(description = "최근 30일 업로드 수", example = "6")
        Integer recentUploadCount30d
) {
}
