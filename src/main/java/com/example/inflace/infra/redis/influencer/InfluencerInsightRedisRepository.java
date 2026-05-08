package com.example.inflace.infra.redis.influencer;

import com.example.inflace.domain.channel.service.insight.InfluencerInsightQueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Repository
@Slf4j
@RequiredArgsConstructor
public class InfluencerInsightRedisRepository {

    private static final String INSIGHT_CACHE_KEY_PREFIX = "influencer:insight:";
    private static final String SUMMARY_CACHE_KEY_PREFIX = "influencer:insight-summary:";
    private static final Duration SUMMARY_CACHE_TTL = Duration.ofHours(6);
    private static final ZoneId CACHE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public InfluencerInsightQueryResult getInsightQueryResult(Long channelId) {
        String value = redisTemplate.opsForValue().get(insightCacheKey(channelId));
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return cacheObjectMapper().readValue(value, InfluencerInsightQueryResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize influencer insight cache. channelId={}", channelId, e);
            redisTemplate.delete(insightCacheKey(channelId));
            return null;
        }
    }

    public void saveInsightQueryResult(Long channelId, InfluencerInsightQueryResult queryResult) {
        try {
            redisTemplate.opsForValue().set(
                    insightCacheKey(channelId),
                    cacheObjectMapper().writeValueAsString(queryResult),
                    ttlUntilNextMidnight()
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize influencer insight cache. channelId={}", channelId, e);
        }
    }

    public String getSummary(Long channelId) {
        return redisTemplate.opsForValue().get(summaryCacheKey(channelId));
    }

    public void saveSummary(Long channelId, String summary) {
        redisTemplate.opsForValue().set(summaryCacheKey(channelId), summary, summaryCacheTtl());
    }

    private String insightCacheKey(Long channelId) {
        return INSIGHT_CACHE_KEY_PREFIX + channelId;
    }

    private String summaryCacheKey(Long channelId) {
        return SUMMARY_CACHE_KEY_PREFIX + channelId;
    }

    private Duration ttlUntilNextMidnight() {
        ZonedDateTime now = ZonedDateTime.now(CACHE_ZONE_ID);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(CACHE_ZONE_ID);
        return Duration.between(now, nextMidnight);
    }

    private Duration summaryCacheTtl() {
        Duration untilNextMidnight = ttlUntilNextMidnight();
        return SUMMARY_CACHE_TTL.compareTo(untilNextMidnight) < 0
                ? SUMMARY_CACHE_TTL
                : untilNextMidnight;
    }

    private ObjectMapper cacheObjectMapper() {
        return objectMapper.copy()
                .disable(MapperFeature.USE_ANNOTATIONS);
    }
}
