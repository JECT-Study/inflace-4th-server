package com.example.inflace.domain.auth.util;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenStore {

    private final ConcurrentHashMap<Long, String> store = new ConcurrentHashMap<>();

    public void save(long userId, String refreshToken) {
        store.put(userId, refreshToken);
    }

    public Optional<String> findByUserId(long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    public void deleteByUserId(long userId) {
        store.remove(userId);
    }

    public boolean isValid(long userId, String refreshToken) {
        return findByUserId(userId)
                .map(stored -> stored.equals(refreshToken))
                .orElse(false);
    }
}
