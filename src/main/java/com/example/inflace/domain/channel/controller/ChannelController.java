package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.response.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.response.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.response.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSubscriberTrendResponse;
import com.example.inflace.domain.channel.dto.response.ChannelTopMainVideosResponse;
import com.example.inflace.domain.channel.dto.response.ChannelTopVideosResponse;
import com.example.inflace.domain.channel.dto.response.ChannelVideosResponse;
import com.example.inflace.domain.channel.dto.response.ChannelSyncResponse;
import com.example.inflace.domain.channel.service.ChannelService;
import com.example.inflace.domain.channel.service.YoutubeChannelSyncService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelController implements ChannelApi {

    private final ChannelService channelService;
    private final YoutubeChannelSyncService youtubeChannelSyncService;

    @PostMapping("/connect")
    public BaseResponse<ChannelSyncResponse> connectMyChannel() {
        return new BaseResponse<>(youtubeChannelSyncService.connectMyChannel());
    }

    @PostMapping("/{channelId}/refresh")
    public BaseResponse<ChannelSyncResponse> refreshChannel(@PathVariable Long channelId) {
        return new BaseResponse<>(youtubeChannelSyncService.refreshChannel(channelId));
    }

    @GetMapping("/{channelId}/main/tops")
    public BaseResponse<ChannelTopMainVideosResponse> getMainTopVideos(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getMainTopVideos(channelId));
    }

    @GetMapping("/{channelId}/tops")
    public BaseResponse<ChannelTopVideosResponse> getTopVideos(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "LONG_FORM") String contentType
    ) {
        return new BaseResponse<>(channelService.getTopVideos(channelId, contentType));
    }

    @GetMapping("/{channelId}/engagement-rate")
    public BaseResponse<ChannelEngagementRateResponse> getEngagementRateVideos(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getEngagementRateVideos(channelId));
    }

    @GetMapping("/{channelId}/new-subscriber")
    public BaseResponse<ChannelNewSubscriberResponse> getNewSubscriberVideos(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getNewSubscriberVideos(channelId));
    }

    @GetMapping("/{channelId}/kpi")
    public BaseResponse<ChannelKpiResponse> getChannelKpi(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getChannelKpi(channelId));
    }

    @GetMapping("/{channelId}/subscriber-pattern")
    public BaseResponse<ChannelSubscriberPatternResponse> getSubscriberPattern(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getSubscriberPattern(channelId));
    }

    @GetMapping("/{channelId}/subscriber-distribution")
    public BaseResponse<ChannelSubscriberDistributionResponse> getSubscriberDistribution(
            @PathVariable Long channelId
    ) {
        return new BaseResponse<>(channelService.getSubscriberDistribution(channelId));
    }

    @GetMapping("/{channelId}/videos")
    public BaseResponse<CursorSliceResponse<ChannelVideosResponse.ChannelVideoItem>> getChannelVideos(
            @PathVariable Long channelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "ALL") String format,
            @RequestParam(required = false) Boolean isAd,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "12") Integer size
    ) {
        return new BaseResponse<>(channelService.getChannelVideos(
                channelId,
                keyword,
                startDate,
                endDate,
                sort,
                format,
                isAd,
                cursor,
                size
        ));
    }

    @GetMapping("/{channelId}/subscriber-trend")
    public BaseResponse<ChannelSubscriberTrendResponse> getSubscriberTrend(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "30D") String range
    ) {
        return new BaseResponse<>(channelService.getSubscriberTrend(channelId, range));
    }
}
