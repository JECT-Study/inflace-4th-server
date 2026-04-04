package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.dto.DropPointsResponse;
import com.example.inflace.domain.video.dto.RetentionSummaryResponse;
import com.example.inflace.domain.video.dto.VideoMetaResponse;
import com.example.inflace.domain.video.dto.VideoStatsResponse;
import com.example.inflace.domain.video.service.VideoService;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos")
public class VideoController implements VideoApi {

    private final VideoService videoService;

    @Override
    @GetMapping("/{videoId}")
    public BaseResponse<VideoMetaResponse> getVideoMeta(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        VideoMetaResponse response = videoService.getVideoMeta(authUser.userId(), videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/stats")
    public BaseResponse<VideoStatsResponse> getVideoStats(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        VideoStatsResponse response = videoService.getVideoStats(authUser.userId(), videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention")
    public BaseResponse<AudienceRetentionResponse> getRetention(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        AudienceRetentionResponse response = videoService.getRetention(authUser.userId(), videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention/drop-points")
    public BaseResponse<DropPointsResponse> getDropPoints(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        DropPointsResponse response = videoService.getDropPoints(authUser.userId(), videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention/summary")
    public BaseResponse<RetentionSummaryResponse> getRetentionSummary(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        RetentionSummaryResponse response = videoService.getRetentionSummary(authUser.userId(), videoId);
        return new BaseResponse<>(response);
    }
}
