package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    @PostMapping("/onboarding")
    public ResponseEntity<BaseResponse> onboarding(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody OnboardingRequest request) {
        userService.onboarding(authUser.userId(), request);
        return ResponseEntity.ok(BaseResponse.success());
    }

    @Override
    @GetMapping("/youtube/linked")
    public BaseResponse<YoutubeLinkedResponse> getYoutubeLinkedStatus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
        return new BaseResponse<>(userService.isYoutubeLinked(authUser.userId()));
    }

    @Override
    @GetMapping("/channels/main")
    public BaseResponse<UserChannelMainResponse> getUserChannelMain(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
        return new BaseResponse<>(userService.getMainChannelInfo(authUser.userId()));
    }

    @Override
    @PostMapping("/youtube/link")
    public BaseResponse<YoutubeChannelLinkResponse> linkYoutubeChannel(
            @AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
        return new BaseResponse<>(userService.linkYoutubeChannel(authUser.userId()));
    }

    @Override
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> withdraw(
            @AuthenticationPrincipal AuthUser authUser) {
        userService.withdraw(authUser.userId());
        return ResponseEntity.ok(new BaseResponse<>(null));
    }
}
