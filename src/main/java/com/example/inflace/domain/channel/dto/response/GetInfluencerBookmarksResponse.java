package com.example.inflace.domain.channel.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GetInfluencerBookmarksResponse(
        @ArraySchema(
                arraySchema = @Schema(description = "현재 로그인 유저가 즐겨찾기한 채널 ID 목록"),
                schema = @Schema(example = "42")
        )
        List<Long> channelIds
) {
}
