package com.example.inflace.domain.channel.service;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.properties.ChannelSyncProperties;
import com.example.inflace.infra.redis.channel.ChannelSyncCooldownRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelSyncCooldownService {

    private static final String CHANNEL_SYNC_COOLDOWN_PREFIX = "channel-sync-cooldown:";
    private static final String COOLDOWN_VALUE = "locked";

    private final ChannelSyncProperties channelSyncProperties;
    private final ChannelSyncCooldownRedisRepository channelSyncCooldownRedisRepository;

    public void validateNotOnCooldown(Long channelId) {
        if (channelSyncCooldownRedisRepository.exists(channelCooldownKey(channelId))) {
            throw new ApiException(ErrorDefine.CHANNEL_SYNC_COOLDOWN);
        }
    }

    public void startCooldown(Long channelId) {
        channelSyncCooldownRedisRepository.save(
                channelCooldownKey(channelId),
                COOLDOWN_VALUE,
                channelSyncProperties.cooldownMillis()
        );
    }

    private String channelCooldownKey(Long channelId) {
        return CHANNEL_SYNC_COOLDOWN_PREFIX + channelId;
    }
}
