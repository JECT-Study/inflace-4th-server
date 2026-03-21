package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/video")
public class VideoController {

    private final VideoService videoService;

    // TODO : 포스트맨 test용 예시 컨트롤러. 차후 기능 개발 용으로는 API DTO 따로 짜서 연동 계획입니다!!
//    @GetMapping("/analytics")
//    public YoutubeAnalyticsVideoResponse getAnalytics(
//            @RequestParam String startDate,
//            @RequestParam String endDate,
//            @RequestParam List<String> metrics
//    ) {
//        return videoService.getYoutubeAnalyticsVideo(
//                new YoutubeAnalyticsVideoRequest(
//                        LocalDate.parse(startDate),
//                        LocalDate.parse(endDate),
//                        metrics
//                )
//        );
//    }
}
