package com.example.inflace.domain.auth.service;

import com.example.inflace.infra.redis.auth.AuthTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenRedisService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final AuthTokenRedisRepository authTokenRedisRepository;

    public void saveRefreshToken(UUID userId, String refreshToken, long expireMillis) {
        authTokenRedisRepository.save(refreshTokenKey(userId), refreshToken, expireMillis);
    }

    public boolean isValidRefreshToken(UUID userId, String refreshToken) {
        String storedRefreshToken = authTokenRedisRepository.get(refreshTokenKey(userId));
        return storedRefreshToken != null && storedRefreshToken.equals(refreshToken);
    }

    public void deleteRefreshToken(UUID userId) {
        authTokenRedisRepository.delete(refreshTokenKey(userId));
    }

    public void saveLogoutAccessToken(String accessToken, long expireMillis) {
        authTokenRedisRepository.saveLogoutAccessToken(accessToken, expireMillis);
    }

    public boolean isLogoutAccessToken(String accessToken) {
        return authTokenRedisRepository.isLogoutAccessToken(accessToken);
    }

    private String refreshTokenKey(UUID userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
