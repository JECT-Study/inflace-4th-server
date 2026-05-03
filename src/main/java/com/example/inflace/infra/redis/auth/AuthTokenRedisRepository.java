package com.example.inflace.infra.redis.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class AuthTokenRedisRepository {

    private static final String LOGOUT_ACCESS_TOKEN_VALUE = "logout";

    private final RedisTemplate<String, String> redisTemplate;

    public void save(String key, String value, long expireMillis) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(expireMillis));
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void saveLogoutAccessToken(String accessToken, long expireMillis) {
        save(accessToken, LOGOUT_ACCESS_TOKEN_VALUE, expireMillis);
    }

    public boolean isLogoutAccessToken(String accessToken) {
        return LOGOUT_ACCESS_TOKEN_VALUE.equals(redisTemplate.opsForValue().get(accessToken));
    }
}
