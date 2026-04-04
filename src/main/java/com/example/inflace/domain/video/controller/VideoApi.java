package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.dto.DropPointsResponse;
import com.example.inflace.domain.video.dto.RetentionSummaryResponse;
import com.example.inflace.domain.video.dto.VideoMetaResponse;
import com.example.inflace.domain.video.dto.VideoStatsResponse;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Video", description = "비디오 API")
public interface VideoApi {
    @Operation(
            summary = "에픽 2-4, 비디오 상세 정보",
            description = "비디오 ID로 영상 메타 정보를 조회합니다. <br>" +
                    "썸네일, 제목, 설명, 해시태그 등을 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.VIDEO_NOT_FOUND)
    BaseResponse<VideoMetaResponse> getVideoMeta(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable("videoId") Long videoId);

    @Operation(
            summary = "에픽 2-4, 비디오 통계",
            description = "비디오 ID로 영상 통계 정보를 조회합니다. <br>" +
                    "조회수, 좋아요, 댓글, 공유수, CTR, 참여율, 신규 유입률, VPH 등 주요 지표를 반환합니다. <br>" +
                    "DB에 데이터가 없을 경우 YouTube Analytics API를 호출하여 저장 후 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.VIDEO_NOT_FOUND)
    BaseResponse<VideoStatsResponse> getVideoStats(@AuthenticationPrincipal AuthUser authUser,
                                                   @PathVariable("videoId") Long videoId);

    @Operation(
            summary = "에픽 2-4, 비디오 시청 지속률 시계열",
            description = "비디오 ID로 시청 지속률 시계열 데이터를 조회합니다. <br>" +
                    "0.01~1.00 구간의 100개 포인트를 반환합니다."
    )
    @ApiErrorDefines({ErrorDefine.VIDEO_NOT_FOUND, ErrorDefine.RETENTION_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<AudienceRetentionResponse> getRetention(@AuthenticationPrincipal AuthUser authUser,
                                                         @PathVariable("videoId") Long videoId);

    @Operation(
            summary = "에픽 2-4, 비디오 이탈 구간",
            description = "비디오 ID로 구간별 평균 이탈률을 조회합니다. <br>" +
                    "100개의 시청 지속률 데이터를 25개씩 4구간으로 나눠 각 구간의 평균 이탈률을 반환합니다."
    )
    @ApiErrorDefines({ErrorDefine.VIDEO_NOT_FOUND, ErrorDefine.RETENTION_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.RETENTION_INVALID, ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<DropPointsResponse> getDropPoints(@AuthenticationPrincipal AuthUser authUser,
                                                   @PathVariable("videoId") Long videoId);

    @Operation(
            summary = "에픽 2-4, 비디오 시청 지속률 요약 통계",
            description = "비디오 ID로 시청 지속률 요약 통계를 조회합니다. <br>" +
                    "평균 시청 지속 시간(초)과 평균 대비 유지율을 반환합니다."
    )
    @ApiErrorDefines({ErrorDefine.VIDEO_NOT_FOUND, ErrorDefine.VIDEO_STATS_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    BaseResponse<RetentionSummaryResponse> getRetentionSummary(@AuthenticationPrincipal AuthUser authUser,
                                                               @PathVariable("videoId") Long videoId);
}