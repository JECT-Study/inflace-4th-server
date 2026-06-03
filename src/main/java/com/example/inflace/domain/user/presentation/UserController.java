package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.auth.util.AuthCookieUtils;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final AuthCookieUtils authCookieUtils;

    @Override
    @GetMapping("/me")
    public BaseResponse<GetUserMeResponse> getUserDetailsInfo() {
        return new BaseResponse<>(userService.getUserDetailsInfo(SecurityUtils.getAuthenticatedUserId()));
    }

    @Override
    @PostMapping("/onboarding")
    public BaseResponse<Void>  onboarding(
            @Valid @RequestBody OnboardingRequest request
    ) {
        userService.onboarding(request);
        return BaseResponse.success();
    }

    @Override
    @GetMapping("/youtube/linked")
    public BaseResponse<YoutubeLinkedResponse> getYoutubeLinkedStatus() {
        return new BaseResponse<>(userService.isYoutubeLinked());
    }

    @Override
    @GetMapping("/channels/main")
    public BaseResponse<UserChannelMainResponse> getUserChannelMain() {
        return new BaseResponse<>(userService.getMainChannelInfo());
    }

    @Override
    @GetMapping("/profile")
    public BaseResponse<UserProfileResponse> getProfile() {
        return new BaseResponse<>(userService.getProfile());
    }

    @Override
    @PutMapping("/preferences")
    public BaseResponse<UserProfileResponse> updatePreferences(
            @Valid @RequestBody UserPreferenceUpdateRequest request
    ) {
        return new BaseResponse<>(userService.updatePreferences(request));
    }

    @Override
    @PostMapping("/profile-image/upload-url")
    public BaseResponse<ProfileImageUploadUrlResponse> createProfileImageUploadUrl(
            @Valid @RequestBody ProfileImageUploadUrlRequest request
    ) {
        return new BaseResponse<>(userService.createProfileImageUploadUrl(request));
    }

    @Override
    @PutMapping("/profile-image")
    public BaseResponse<UserProfileResponse> updateProfileImage(
            @Valid @RequestBody ProfileImageUpdateRequest request
    ) {
        return new BaseResponse<>(userService.updateProfileImage(request));
    }

    @Override
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtils.buildDeleteRefreshTokenCookie().toString())
                .body(new BaseResponse<>(null));
    }

    @Override
    @GetMapping("/alarms")
    public BaseResponse<UserAlarmsResponse> getAlarms() {
        return new BaseResponse<>(userService.getAlarms());
    }

    @Override
    @PutMapping("/alarms")
    public BaseResponse<UserAlarmUpdateResponse> updateAlarm(
            @Valid @RequestBody UserAlarmUpdateRequest request
    ) {
        return new BaseResponse<>(userService.updateAlarm(request));
    }

    @Override
    @PutMapping("/alarms/email")
    public BaseResponse<UserAlarmEmailUpdateResponse> updateAlarmEmail(
            @Valid @RequestBody UserAlarmEmailUpdateRequest request
    ) {
        return new BaseResponse<>(userService.updateAlarmEmail(request));
    }
}
