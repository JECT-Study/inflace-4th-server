package com.example.inflace.domain.channel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetInfluencerInsightSummaryResponse(
        @Schema(
                description = "LLM이 생성한 채널 인사이트 요약",
                nullable = true,
                example = "팬층 반응이 안정적이고 최근 콘텐츠 성장세가 강한 채널입니다. 광고 지표도 무난한 편이라 협찬 적합도가 높습니다."
        )
        String summary
) {
}
