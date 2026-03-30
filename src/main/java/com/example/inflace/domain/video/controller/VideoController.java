package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.dto.VideoMetaResponse;
import com.example.inflace.domain.video.dto.VideoStatsResponse;
import com.example.inflace.domain.video.service.VideoService;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/video")
public class VideoController implements VideoApi {

    private final VideoService videoService;

    @Override
    @GetMapping("/{videoId}")
    public BaseResponse<VideoMetaResponse> getVideoMeta(
            @AuthenticationPrincipal String email,  // 현재 jwt에 저장되는 컨텍스트가 이메일
            @PathVariable Long videoId
    ) {
        VideoMetaResponse response = videoService.getVideoMeta(videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/stats")
    public BaseResponse<VideoStatsResponse> getVideoStats(
            @AuthenticationPrincipal String email,
            @PathVariable Long videoId
    ) {
        VideoStatsResponse response = videoService.getVideoStats(email, videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention")
    public BaseResponse<AudienceRetentionResponse> getRetention(
            @AuthenticationPrincipal String email,
            @PathVariable Long videoId
    ) {
        AudienceRetentionResponse response = videoService.getRetention(email, videoId);
        return new BaseResponse<>(response);
    }
}
