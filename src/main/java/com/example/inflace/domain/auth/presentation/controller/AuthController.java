package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.facade.AuthFacade;
import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new BaseResponse<>(new AuthResponse(response.accessToken())));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @AuthenticationPrincipal AuthUser authUser) {
        authFacade.logout(authUser.userId());

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new BaseResponse<>(null));
    }

    @Override
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<AuthResponse>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        TokenData tokenData = authFacade.reissue(refreshToken);

        ResponseCookie newCookie = ResponseCookie.from("refreshToken", tokenData.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24 * 7)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newCookie.toString())
                .body(new BaseResponse<>(new AuthResponse(tokenData.accessToken())));
    }
}
