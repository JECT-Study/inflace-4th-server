package com.example.inflace.domain.user.presentation;

import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 API")
public interface UserApi {

    @Operation(
            summary = "온보딩",
            description = "유저 역할과 필요 항목을 저장합니다."
    )
    BaseResponse<Void> onboarding(@RequestBody OnboardingRequest request);

    @Operation(
            summary = "에픽 2-1, 유튜브 채널 연동 여부 조회",
            description = "현재 로그인한 유저의 유튜브 채널 연동 여부를 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.AUTH_FORBIDDEN)
    BaseResponse<YoutubeLinkedResponse> getYoutubeLinkedStatus();

    @Operation(
            summary = "에픽 2-1, 유저 채널 메인 정보 조회",
            description = "유저 본인의 YouTube 채널 기본 정보를 반환합니다."
    )
    @ApiErrorDefines({ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.USER_NOT_FOUND, ErrorDefine.CHANNEL_NOT_FOUND, ErrorDefine.CHANNEL_STATS_NOT_FOUND})
    BaseResponse<UserChannelMainResponse> getUserChannelMain();

    @Operation(
            summary = "마이페이지 프로필 조회",
            description = "마이페이지 정보를 조회합니다."
    )
    @ApiErrorDefines({ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.USER_NOT_FOUND})
    BaseResponse<UserProfileResponse> getProfile();

    @Operation(
            summary = "마이페이지 맞춤 서비스 정보 수정",
            description = "현재 로그인한 유저의 userType과 userNeed를 수정합니다."
    )
    @ApiErrorDefines({ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.USER_NOT_FOUND})
    BaseResponse<UserProfileResponse> updatePreferences(@RequestBody UserPreferenceUpdateRequest request);

    @Operation(
            summary = "마이페이지 프로필 이미지 업로드 URL 발급",
            description = "현재 로그인한 유저의 프로필 이미지를 S3에 직접 업로드할 수 있는 presigned URL을 발급합니다."
    )
    @ApiErrorDefines({ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.INVALID_PROFILE_IMAGE, ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED})
    BaseResponse<ProfileImageUploadUrlResponse> createProfileImageUploadUrl(@RequestBody ProfileImageUploadUrlRequest request);

    @Operation(
            summary = "마이페이지 프로필 이미지 수정",
            description = "S3 업로드가 완료된 프로필 이미지 object key를 검증하고 현재 로그인한 유저의 프로필 이미지 URL을 수정합니다."
    )
    @ApiErrorDefines({ErrorDefine.AUTH_FORBIDDEN, ErrorDefine.USER_NOT_FOUND, ErrorDefine.INVALID_PROFILE_IMAGE, ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED})
    BaseResponse<UserProfileResponse> updateProfileImage(@RequestBody ProfileImageUpdateRequest request);

    @Operation(
            summary = "회원 탈퇴",
            description = "탈퇴 사유를 기재하고 현재 로그인된 유저를 탈퇴 처리합니다."
    )
    @ApiErrorDefines(ErrorDefine.USER_NOT_FOUND)
    ResponseEntity<BaseResponse<Void>> withdraw(@RequestBody WithdrawRequest request);
}
