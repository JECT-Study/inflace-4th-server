package com.example.inflace.domain.brandcollaboration.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BrandCollaborationVideoResponse(
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

        @Schema(description = "채널 고유 ID", example = "UC_x5XG1OV2P6uZZ5FSM9Ttw")
        String channelId,

        @Schema(description = "채널명 (크리에이터명)", example = "뷰티크리에이터A")
        String channelName,

        @Schema(description = "채널 프로필 썸네일 URL", example = "https://yt3.ggpht.com/example=s176-c-k-c0x00ffffff-no-rj")
        String channelThumbnailUrl
) {}
