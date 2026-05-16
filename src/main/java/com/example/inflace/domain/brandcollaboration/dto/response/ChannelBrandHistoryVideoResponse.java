package com.example.inflace.domain.brandcollaboration.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ChannelBrandHistoryVideoResponse(
        @Schema(description = "유튜브 영상 고유 ID", example = "abc123xyz")
        String videoId,

        @Schema(description = "영상 제목", example = "[광고] 신상 앰플 리뷰")
        String videoTitle,

        @Schema(description = "영상 썸네일 URL", example = "https://i.ytimg.com/vi/abc123xyz/hqdefault.jpg")
        String videoThumbnailUrl,

        @Schema(description = "업로드 일자 (ISO 8601)", example = "2024-05-01T10:00:00Z")
        String publishedAt,

        @Schema(description = "영상 조회수", example = "450000")
        long viewCount,

        @Schema(description = "좋아요 수", example = "12500")
        long likeCount,

        @Schema(description = "댓글 수", example = "850")
        long commentCount,

        @Schema(description = "영상 형식", allowableValues = {"SHORT_FORM", "LONG_FORM"}, example = "LONG_FORM")
        String videoFormat,

        @Schema(description = "영상 카테고리명", example = "뷰티/패션")
        String categoryName,

        @Schema(description = "브랜드 목록 (snippet.description 파싱 후 alias 매핑)", example = "[\"APR\", \"메디큐브\"]")
        List<String> brands
) {}
