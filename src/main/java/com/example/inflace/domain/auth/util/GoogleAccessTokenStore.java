package com.example.inflace.domain.auth.util;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoogleAccessTokenStore {
    private final Map<Long, String> tokenStore = new ConcurrentHashMap<>();

    public void save(long userId, String accessToken) {
        tokenStore.put(userId, accessToken);
    }

    public String getAccessToken(long userId) {
        String token = tokenStore.get(userId);
        if (token == null) {
            throw new ApiException(ErrorDefine.INVALID_HEADER_ERROR);
        }
        return token;
    }
}
