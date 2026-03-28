package com.example.inflace.domain.video.controller;

import com.example.inflace.domain.video.dto.VideoMetaResponse;
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
            description = "비디오 ID로 영상 메타 정보를 조회합니다. " +
                    "썸네일, 제목, 설명, 해시태그 등을 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.VIDEO_NOT_FOUND)
    BaseResponse<VideoMetaResponse> getVideoMeta(@AuthenticationPrincipal String googleId,
                                                 @PathVariable("videoId") Long videoId);
}