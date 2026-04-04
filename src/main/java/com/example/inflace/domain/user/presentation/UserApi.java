package com.example.inflace.domain.user.presentation;

import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 API")
public interface UserApi {

    @Operation(
            summary = "온보딩",
            description = "유저 역할과 필요 항목을 저장합니다."
    )
    ResponseEntity<BaseResponse> onboarding(@AuthenticationPrincipal AuthUser authUser,
                                            @RequestBody OnboardingRequest request);

    @Operation(
            summary = "에픽 2-1, 유튜브 채널 연동 여부 조회",
            description = "현재 로그인한 유저의 유튜브 채널 연동 여부를 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.AUTH_FORBIDDEN)
    BaseResponse<YoutubeLinkedResponse> getYoutubeLinkedStatus(@AuthenticationPrincipal AuthUser authUser);
}
