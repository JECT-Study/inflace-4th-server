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
    public BaseResponse<Void>  onboarding(
            @RequestBody OnboardingRequest request
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
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> withdraw() {
        userService.withdraw();
        return ResponseEntity.ok(new BaseResponse<>(null));
    }
}
