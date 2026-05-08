package com.example.inflace.domain.idempotency.service;

import com.example.inflace.domain.idempotency.model.IdempotencyKeyMetadata;
import com.example.inflace.infra.redis.idempotency.IdempotencyRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRedisRepository idempotencyRedisRepository;

    public boolean saveIfAbsent(String idempotencyKey, IdempotencyKeyMetadata metadata) {
        return idempotencyRedisRepository.saveIfAbsent(idempotencyKey, metadata);
    }
}
