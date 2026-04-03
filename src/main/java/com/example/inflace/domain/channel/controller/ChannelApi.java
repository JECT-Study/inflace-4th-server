package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberTrendResponse;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.domain.channel.dto.ChannelVideosResponse;
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

    @Operation(
            summary = "구독자/비구독자 비율",
            description = "채널의 구독자 조회수와 비구독자 조회수 비율을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.CHANNEL_STATS_NOT_FOUND})
    BaseResponse<ChannelSubscriberPatternResponse> getSubscriberPattern(@PathVariable Long channelId);

    @Operation(
            summary = "구독자 분포",
            description = "채널의 국가별, 연령별, 성별 분포를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.CHANNEL_STATS_NOT_FOUND})
    BaseResponse<ChannelSubscriberDistributionResponse> getSubscriberDistribution(@PathVariable Long channelId);

    @Operation(
            summary = "영상 목록 조회",
            description = "내 채널의 영상목록을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<ChannelVideosResponse> getChannelVideos(
            @PathVariable Long channelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") String sort,
            @RequestParam(required = false, defaultValue = "ALL") String format,
            @RequestParam(required = false) Boolean isAd,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "12") Integer size
    );

    @Operation(
            summary = "구독자 추이",
            description = "범위별 구독자 추이 6개 포인트를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<ChannelSubscriberTrendResponse> getSubscriberTrend(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "30D") String range
    );
}
