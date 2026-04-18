package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.dto.DropPointsResponse;
import com.example.inflace.domain.video.dto.RetentionSummaryResponse;
import com.example.inflace.domain.video.dto.VideoMetaResponse;
import com.example.inflace.domain.video.dto.VideoStatsResponse;
import com.example.inflace.domain.video.service.VideoService;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos")
public class VideoController implements VideoApi {

    private final VideoService videoService;

    @Override
    @GetMapping("/{videoId}")
    public BaseResponse<VideoMetaResponse> getVideoMeta(@PathVariable Long videoId) {
        VideoMetaResponse response = videoService.getVideoMeta(videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/stats")
    public BaseResponse<VideoStatsResponse> getVideoStats(@PathVariable Long videoId) {
        VideoStatsResponse response = videoService.getVideoStats(videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention")
    public BaseResponse<AudienceRetentionResponse> getRetention(@PathVariable Long videoId) {
        AudienceRetentionResponse response = videoService.getRetention(videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention/drop-points")
    public BaseResponse<DropPointsResponse> getDropPoints(@PathVariable Long videoId) {
        DropPointsResponse response = videoService.getDropPoints(videoId);
        return new BaseResponse<>(response);
    }

    @Override
    @GetMapping("/{videoId}/retention/summary")
    public BaseResponse<RetentionSummaryResponse> getRetentionSummary(@PathVariable Long videoId) {
        RetentionSummaryResponse response = videoService.getRetentionSummary(videoId);
        return new BaseResponse<>(response);
    }
}
