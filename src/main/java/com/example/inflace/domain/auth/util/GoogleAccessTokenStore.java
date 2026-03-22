package com.example.inflace.domain.auth.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoogleAccessTokenStore {
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    public void save(String googleId, String accessToken) {
        tokenStore.put(googleId, accessToken);
    }

    public String getAccessToken(String googleId) {
        return tokenStore.get(googleId);
    }
}
