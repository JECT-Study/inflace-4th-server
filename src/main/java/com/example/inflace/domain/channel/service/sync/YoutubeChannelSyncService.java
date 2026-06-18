package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import com.example.inflace.domain.channel.dto.ChannelDataSyncResult;
import com.example.inflace.domain.channel.dto.response.ChannelSyncResponse;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelAnalyticsRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeChannelSyncService {

    private static final String MY_CHANNEL_PARTS = "snippet,statistics,contentDetails";

    private final UserReadRepository userReadRepository;
    private final ChannelRepository channelRepository;
    private final ChannelAnalyticsRepository channelAnalyticsRepository;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final YoutubeChannelDataSyncService youtubeChannelDataSyncService;
    private final YoutubeChannelAnalyticsSyncService youtubeChannelAnalyticsSyncService;
    private final ChannelSyncCooldownService channelSyncCooldownService;

    @Transactional
    public ChannelSyncResponse connectMyChannel() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        YoutubeDataChannelResponse response = youtubeDataApiClient.getMyChannel(user.getProviderId(), MY_CHANNEL_PARTS);
        YoutubeDataChannelResponse.Item myChannel = extractMyChannel(response);

        channelRepository.findByUser_IdAndYoutubeChannelId(userId, myChannel.id())
                .map(Channel::getId)
                .ifPresent(channelSyncCooldownService::validateNotOnCooldown);

        ChannelDataSyncResult result = youtubeChannelDataSyncService.synchronizeChannel(
                user,
                user.getProviderId(),
                myChannel
        );
        youtubeChannelAnalyticsSyncService.syncAnalytics(user.getProviderId(), result.channel(), result.videos());
        channelSyncCooldownService.startCooldown(result.channel().getId());
        return buildChannelSyncResponse(result.channel());
    }

    @Transactional
    public ChannelSyncResponse refreshChannel(Long channelId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        validateChannelOwnership(channel, userId);
        channelSyncCooldownService.validateNotOnCooldown(channelId);

        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(
                channel.getYoutubeChannelId(),
                MY_CHANNEL_PARTS
        );
        YoutubeDataChannelResponse.Item channelItem = extractMyChannel(response);

        ChannelDataSyncResult result = youtubeChannelDataSyncService.synchronizeChannel(
                user,
                user.getProviderId(),
                channelItem
        );
        youtubeChannelAnalyticsSyncService.syncAnalytics(user.getProviderId(), result.channel(), result.videos());
        channelSyncCooldownService.startCooldown(result.channel().getId());
        return buildChannelSyncResponse(result.channel());
    }

    private YoutubeDataChannelResponse.Item extractMyChannel(YoutubeDataChannelResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            throw new ApiException(ErrorDefine.CHANNEL_NOT_FOUND);
        }
        return response.items().get(0);
    }

    private void validateChannelOwnership(Channel channel, UUID userId) {
        if (!channel.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }

    private ChannelSyncResponse buildChannelSyncResponse(Channel channel) {
        LocalDateTime updatedAt = channelAnalyticsRepository.findByChannel_Id(channel.getId())
                .map(ChannelAnalytics::getUpdatedAt)
                .orElse(LocalDateTime.now());
        return new ChannelSyncResponse(channel.getId(), channel.getYoutubeChannelId(), updatedAt);
    }
}
