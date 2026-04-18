package com.example.inflace.domain.auth.util;

import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieUtils {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(14);

    private final CookieProperties cookieProperties;

    public ResponseCookie buildRefreshTokenCookie(TokenData tokenData) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokenData.refreshToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .sameSite(cookieProperties.sameSite())
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();
    }

    public ResponseCookie buildDeleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .sameSite(cookieProperties.sameSite())
                .maxAge(Duration.ZERO)
                .build();
    }

    public String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
    }
}
