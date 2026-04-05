package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.service.VideoSyncService;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 테스트용 샘플 컨트롤러. 차후 온보딩 연동 버튼으로 교체 예정
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test/videos")
public class VideoSyncController {

    private final VideoSyncService videoSyncService;

    @PostMapping("/{videoId}/sync")
    public BaseResponse<Void> syncVideo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long videoId
    ) {
        videoSyncService.syncStats(authUser.userId(), videoId);
        videoSyncService.syncRetention(authUser.userId(), videoId);
//        videoSyncService.syncUnsubscribedStats(authUser.userId(), videoId);
        return new BaseResponse<>(null);
    }
}
