package com.example.inflace.domain.auth.local.controller;

import com.example.inflace.domain.auth.local.facade.LocalAuthFacade;
import com.example.inflace.domain.auth.presentation.dto.AuthFacadeLoginResponse;
import com.example.inflace.domain.auth.presentation.dto.LoginRequest;
import com.example.inflace.domain.auth.presentation.dto.LoginResponse;
import com.example.inflace.domain.auth.util.AuthCookieUtils;
import com.example.inflace.global.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local/auth")
@RequiredArgsConstructor
public class LocalAuthController {

    private final LocalAuthFacade localAuthFacade;
    private final AuthCookieUtils authCookieUtils;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthFacadeLoginResponse response = localAuthFacade.login(request);
        String cookie = authCookieUtils.buildRefreshTokenCookie(response.tokenData()).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(new BaseResponse<>(new LoginResponse(
                        response.tokenData().accessToken(),
                        response.userDetails(),
                        response.userChannelDetails()
                )));
    }
}
