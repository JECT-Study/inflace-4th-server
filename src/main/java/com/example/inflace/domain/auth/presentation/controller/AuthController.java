package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.facade.AuthFacade;
import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.domain.auth.util.AuthCookieUtils;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.properties.CorsAllowedOriginsProperties;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.util.EnvironmentUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController implements AuthApi {

    private final AuthFacade authFacade;
    private final AuthCookieUtils authCookieUtils;
    private final CorsAllowedOriginsProperties corsAllowedOriginsProperties;
    private final Environment environment;
    
    @Override
    @GetMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(
            @RequestParam("provider") String provider,
            @RequestParam("code") String code
    ) {
        TokenData tokenData = authFacade.login(provider, code);

        String cookie = authCookieUtils.buildRefreshTokenCookie(tokenData).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(new BaseResponse<>(new AuthResponse(tokenData.accessToken())));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request) {
        authFacade.logout(extractAccessToken(request));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtils.buildDeleteRefreshTokenCookie().toString())
                .body(new BaseResponse<>(null));
    }

    @Override
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<AuthResponse>> reissue(
            HttpServletRequest request
    ) {
        if (!EnvironmentUtils.isLocal(environment)) throwIfNotAllowedOrigin(request);

        String refreshToken = authCookieUtils.extractRefreshToken(request.getCookies());
        log.info("reissue request accepted: hasRefreshToken={}", refreshToken != null && !refreshToken.isBlank());
        TokenData tokenData = authFacade.reissue(refreshToken);

        String cookie = authCookieUtils.buildRefreshTokenCookie(tokenData).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(new BaseResponse<>(new AuthResponse(tokenData.accessToken())));
    }

    private void throwIfNotAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (origin == null || origin.isBlank()) {
            if (referer != null && !referer.isBlank()) {
                try {
                    URI uri = URI.create(referer);
                    origin = uri.getScheme() + "://" + uri.getHost();
                } catch (IllegalArgumentException ignored) {
                    origin = null;
                }
            }
        }

        log.info(
                "reissue origin check: originHeader={}, referer={}, resolvedOrigin={}, allowedOrigins={}",
                request.getHeader(HttpHeaders.ORIGIN),
                referer,
                origin,
                corsAllowedOriginsProperties.getOrigins()
        );

        if (origin == null) {
            log.warn("reissue forbidden: resolved origin is null");
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }

        String finalOrigin = origin;
        boolean allowed = corsAllowedOriginsProperties.getOrigins().stream()
                .filter(Objects::nonNull)
                .anyMatch(allowedOrigin -> allowedOrigin.equals(finalOrigin));

        if (!allowed) {
            log.warn("reissue forbidden: origin {} is not in allowed origins {}", finalOrigin, corsAllowedOriginsProperties.getOrigins());
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
