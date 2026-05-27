package com.example.inflace.domain.auth.util;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.infra.redis.auth.AuthTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleAccessTokenStore {
    private static final String GOOGLE_ACCESS_TOKEN_PREFIX = "auth:google-access:";
    private static final String GOOGLE_REFRESH_TOKEN_PREFIX = "auth:google-refresh:";
    private static final long DEFAULT_GOOGLE_ACCESS_TOKEN_EXPIRE_MILLIS = 60L * 60L * 1000L;

    private final AuthTokenRedisRepository authTokenRedisRepository;

    public void saveTokens(String googleId, String accessToken, String refreshToken, Long expiresInSeconds) {
        saveAccessToken(googleId, accessToken, expiresInSeconds);
        if (refreshToken != null) {
            saveRefreshToken(googleId, refreshToken);
        }
    }

    public void saveAccessToken(String googleId, String accessToken, Long expiresInSeconds) {
        long expireMillis = expiresInSeconds != null
                ? expiresInSeconds * 1000L
                : DEFAULT_GOOGLE_ACCESS_TOKEN_EXPIRE_MILLIS;

        authTokenRedisRepository.save(googleAccessTokenKey(googleId), accessToken, expireMillis);
    }

    public void saveRefreshToken(String googleId, String refreshToken) {
        authTokenRedisRepository.save(googleRefreshTokenKey(googleId), refreshToken);
    }

    public String findAccessToken(String googleId) {
        return authTokenRedisRepository.get(googleAccessTokenKey(googleId));
    }

    public String getRefreshToken(String googleId) {
        String refreshToken = findRefreshToken(googleId);
        if (refreshToken == null) {
            throw new ApiException(ErrorDefine.REFRESH_TOKEN_NOT_FOUND);
        }
        return refreshToken;
    }

    public String findRefreshToken(String googleId) {
        return authTokenRedisRepository.get(googleRefreshTokenKey(googleId));
    }

    public void deleteTokens(String googleId) {
        authTokenRedisRepository.delete(googleAccessTokenKey(googleId));
        authTokenRedisRepository.delete(googleRefreshTokenKey(googleId));
    }

    private String googleAccessTokenKey(String googleId) {
        return GOOGLE_ACCESS_TOKEN_PREFIX + googleId;
    }

    private String googleRefreshTokenKey(String googleId) {
        return GOOGLE_REFRESH_TOKEN_PREFIX + googleId;
    }
}
