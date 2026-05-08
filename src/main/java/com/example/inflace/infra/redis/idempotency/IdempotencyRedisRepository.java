package com.example.inflace.infra.redis.idempotency;

import com.example.inflace.domain.idempotency.model.IdempotencyKeyMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@Slf4j
@RequiredArgsConstructor
public class IdempotencyRedisRepository {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public boolean saveIfAbsent(String idempotencyKey, IdempotencyKeyMetadata metadata) {
        try {
            Boolean saved = redisTemplate.opsForValue().setIfAbsent(
                    redisKey(idempotencyKey),
                    objectMapper.writeValueAsString(metadata),
                    IDEMPOTENCY_TTL
            );
            return Boolean.TRUE.equals(saved);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency metadata. key={}", idempotencyKey, e);
            throw new IllegalStateException("Failed to serialize idempotency metadata", e);
        }
    }

    private String redisKey(String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
    }
}
