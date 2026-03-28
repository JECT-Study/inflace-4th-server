package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.service.ChannelService;
import com.example.inflace.domain.video.dto.ChannelTopVideosResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping("/{channelId}/tops")
    public BaseResponse<ChannelTopVideosResponse> getTopVideos(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "LONG_FORM") String contentType
    ) {
        return new BaseResponse<>(channelService.getTopVideos(channelId, contentType));
    }
}
