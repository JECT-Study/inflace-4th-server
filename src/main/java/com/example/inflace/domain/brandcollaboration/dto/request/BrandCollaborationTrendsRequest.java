package com.example.inflace.domain.brandcollaboration.dto.request;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.util.StringUtils;

public record BrandCollaborationTrendsRequest(
        @ArraySchema(schema = @Schema(description = "분석할 유튜브 영상 ID 목록", example = "dQw4w9WgXcQ"))
        List<String> youtubeVideoIds
) {
    private static final int MAX_VIDEO_COUNT = 10;

    public BrandCollaborationTrendsRequest {
        if (youtubeVideoIds == null || youtubeVideoIds.isEmpty()) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
        if (youtubeVideoIds.size() > MAX_VIDEO_COUNT) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
        boolean hasBlank = youtubeVideoIds.stream().anyMatch(id -> !StringUtils.hasText(id));
        if (hasBlank) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }
}
