package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Channel", description = "채널 관련 API")
public interface ChannelApi {

    @Operation(
            summary = "인기 급상승 영상 Top 5",
            description = "영상 타입별로 채널의 인기 급상승 Top 5 영상을 조회합니다. "
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<ChannelTopVideosResponse> getTopVideos(
            @PathVariable Long channelId,
            @RequestParam String contentType
    );

    @Operation(
            summary = "참여율 차트",
            description = "채널의 롱폼/쇼츠 평균 참여율과 영상별 참여율 Top 5 리스트를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<ChannelEngagementRateResponse> getEngagementRateVideos(@PathVariable Long channelId);

    @Operation(
            summary = "신규 유입 비율 TOP 영상",
            description = "채널의 신규 유입 비율이 높은 상위 5개 영상을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<ChannelNewSubscriberResponse> getNewSubscriberVideos(@PathVariable Long channelId);

    @Operation(
            summary = "핵심 지표 카드(KPI)",
            description = "채널의 핵심 지표 카드를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.CHANNEL_STATS_NOT_FOUND})
    BaseResponse<ChannelKpiResponse> getChannelKpi(@PathVariable Long channelId);
}
