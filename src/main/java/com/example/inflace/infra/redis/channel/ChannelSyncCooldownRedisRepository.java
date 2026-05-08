package com.example.inflace.infra.redis.channel;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChannelSyncCooldownRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void save(String key, String value, long expireMillis) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(expireMillis));
    }
}
