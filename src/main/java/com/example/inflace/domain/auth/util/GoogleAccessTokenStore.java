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
    private static final long DEFAULT_GOOGLE_ACCESS_TOKEN_EXPIRE_MILLIS = 60L * 60L * 1000L;

    private final AuthTokenRedisRepository authTokenRedisRepository;

    public void save(String googleId, String accessToken, Long expiresInSeconds) {
        long expireMillis = expiresInSeconds != null
                ? expiresInSeconds * 1000L
                : DEFAULT_GOOGLE_ACCESS_TOKEN_EXPIRE_MILLIS;

        authTokenRedisRepository.save(googleAccessTokenKey(googleId), accessToken, expireMillis);
    }

    public String getAccessToken(String googleId) {
        String token = authTokenRedisRepository.get(googleAccessTokenKey(googleId));
        if (token == null) {
            throw new ApiException(ErrorDefine.INVALID_HEADER_ERROR);
        }
        return token;
    }

    private String googleAccessTokenKey(String googleId) {
        return GOOGLE_ACCESS_TOKEN_PREFIX + googleId;
    }
}
