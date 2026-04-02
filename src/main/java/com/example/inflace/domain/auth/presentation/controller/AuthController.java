package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.facade.AuthFacade;
import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthFacade authFacade;

    @Override
    @GetMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(
            @RequestParam("provider") String provider,
            @RequestParam("code") String code
    ) {
        TokenData response = authFacade.login(provider, code);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24 * 7)
                .sameSite("Lax")
                .build();

        AuthResponse newResponse = new AuthResponse(response.accessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new BaseResponse<>(newResponse));
    }
}