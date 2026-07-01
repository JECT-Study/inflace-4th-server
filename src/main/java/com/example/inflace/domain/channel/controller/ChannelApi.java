package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.response.ChannelSyncResponse;
import com.example.inflace.domain.channel.dto.response.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.response.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.response.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberTrendResponse;
import com.example.inflace.domain.channel.dto.response.ChannelTopMainVideosResponse;
import com.example.inflace.domain.channel.dto.response.ChannelTopVideosResponse;
import com.example.inflace.domain.channel.dto.response.ChannelVideosResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Channel", description = "채널 관련 API")
public interface ChannelApi {

    @Operation(
            summary = "유튜브 채널 연동",
            description = "유튜브 채널을 연동합니다."
    )
    @ApiErrorDefines({ErrorDefine.USER_NOT_FOUND, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.YOUTUBE_API_ERROR})
    BaseResponse<ChannelSyncResponse> connectMyChannel();

    @Operation(
            summary = "새로고침",
            description = "유튜브의 정보를 새로고침 합니다."
    )
    @ApiErrorDefines({ErrorDefine.USER_NOT_FOUND, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.YOUTUBE_API_ERROR})
    BaseResponse<ChannelSyncResponse> refreshChannel(@PathVariable Long channelId);

    @Operation(
            summary = "에픽 2-1, 메인 인기 영상 Top 5",
            description = "쇼츠/일반 구분 없이 채널의 인기 Top 5 영상을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<ChannelTopMainVideosResponse> getMainTopVideos(@PathVariable Long channelId);

    @Operation(
            summary = "인기 급상승 영상 Top 5",
            description = "영상 타입별로 채널의 인기 급상승 Top 5 영상을 조회합니다. "
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<ChannelTopVideosResponse> getTopVideos(
            @PathVariable Long channelId,
            @RequestParam String contentType
    );

    @Operation(
            summary = "참여율 차트",
            description = "채널의 롱폼/쇼츠 평균 참여율과 영상별 참여율 Top 5 리스트를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<ChannelEngagementRateResponse> getEngagementRateVideos(@PathVariable Long channelId);

    @Operation(
            summary = "신규 유입 비율 TOP 영상",
            description = "채널의 신규 유입 비율이 높은 상위 5개 영상을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.ANALYTICS_DATA_NOT_FOUND})
    BaseResponse<ChannelNewSubscriberResponse> getNewSubscriberVideos(@PathVariable Long channelId);

    @Operation(
            summary = "핵심 지표 카드(KPI)",
            description = "채널의 핵심 지표 카드를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.CHANNEL_STATS_NOT_FOUND})
    BaseResponse<ChannelKpiResponse> getChannelKpi(@PathVariable Long channelId);

    @Operation(
            summary = "구독자/비구독자 비율",
            description = "채널의 구독자 조회수와 비구독자 조회수 비율을 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.CHANNEL_STATS_NOT_FOUND, ErrorDefine.CHANNEL_ANALYTICS_NOT_FOUND, ErrorDefine.ANALYTICS_DATA_NOT_FOUND})
    BaseResponse<ChannelSubscriberPatternResponse> getSubscriberPattern(@PathVariable Long channelId);

    @Operation(
            summary = "구독자 분포",
            description = "채널의 국가별, 연령별, 성별 분포를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.CHANNEL_ANALYTICS_NOT_FOUND, ErrorDefine.ANALYTICS_DATA_NOT_FOUND})
    BaseResponse<ChannelSubscriberDistributionResponse> getSubscriberDistribution(@PathVariable Long channelId);

    @Operation(
            summary = "영상 목록 조회",
            description = "내 채널의 영상목록을 조회합니다. 기간 필터는 yyyy-MM-dd 형식의 startDate/endDate로 전달합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.INVALID_DATE_FORMAT, ErrorDefine.INVALID_DATE_RANGE})
    BaseResponse<CursorSliceResponse<ChannelVideosResponse.ChannelVideoItem>> getChannelVideos(
            @PathVariable Long channelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
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
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<ChannelSubscriberTrendResponse> getSubscriberTrend(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "30D") String range
    );

    @Operation(
            summary = "채널 연동 해제",
            description = "연동된 유튜브 채널을 해제합니다."
    )
    @ApiErrorDefines({ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<Void> disconnectChannel(@PathVariable Long channelId);
}
