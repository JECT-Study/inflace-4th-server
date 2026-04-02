package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.ChannelVideosResponse;
import com.example.inflace.domain.channel.service.ChannelService;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelController implements ChannelApi{

    private final ChannelService channelService;

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
    public BaseResponse<ChannelVideosResponse> getChannelVideos(
            @PathVariable Long channelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "12") Integer size
    ) {
        return new BaseResponse<>();
    }
}
