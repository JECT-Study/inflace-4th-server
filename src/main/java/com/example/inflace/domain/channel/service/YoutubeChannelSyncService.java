package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.ChannelDataSyncResult;
import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
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
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final YoutubeChannelDataSyncService youtubeChannelDataSyncService;
    private final YoutubeChannelAnalyticsSyncService youtubeChannelAnalyticsSyncService;

    @Transactional
    public Long connectMyChannel() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        YoutubeDataChannelResponse response = youtubeDataApiClient.getMyChannel(user.getProviderId(), MY_CHANNEL_PARTS);
        YoutubeDataChannelResponse.Item myChannel = extractMyChannel(response);

        ChannelDataSyncResult result = youtubeChannelDataSyncService.synchronizeChannel(
                user,
                user.getProviderId(),
                myChannel
        );
        youtubeChannelAnalyticsSyncService.syncAnalytics(user.getProviderId(), result.channel(), result.videos());
        return result.channel().getId();
    }

    @Transactional
    public Long refreshChannel(Long channelId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        validateChannelOwnership(channel, userId);

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
        return result.channel().getId();
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
}
