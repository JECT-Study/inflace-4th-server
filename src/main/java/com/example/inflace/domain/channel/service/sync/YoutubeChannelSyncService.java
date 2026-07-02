package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import com.example.inflace.domain.channel.dto.ChannelDataSyncResult;
import com.example.inflace.domain.channel.dto.sync.ConnectChannelTarget;
import com.example.inflace.domain.channel.dto.sync.RefreshChannelTarget;
import com.example.inflace.domain.channel.dto.response.ChannelSyncResponse;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelAnalyticsRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.global.security.util.SecurityUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YoutubeChannelSyncService {

    private final ChannelRepository channelRepository;
    private final ChannelAnalyticsRepository channelAnalyticsRepository;
    private final ChannelSyncQueryService channelSyncQueryService;
    private final YoutubeChannelDataFetchService youtubeChannelDataFetchService;
    private final YoutubeChannelDataPersistenceService youtubeChannelDataPersistenceService;
    private final YoutubeChannelAnalyticsSyncService youtubeChannelAnalyticsSyncService;
    private final ChannelSyncCooldownService channelSyncCooldownService;

    public ChannelSyncResponse connectMyChannel() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        ConnectChannelTarget target = channelSyncQueryService.getConnectTarget(userId);
        YoutubeDataChannelResponse.Item myChannel = youtubeChannelDataFetchService.fetchMyChannel(target.googleId());

        channelRepository.findByUser_IdAndYoutubeChannelId(target.userId(), myChannel.id())
                .map(Channel::getId)
                .ifPresent(channelSyncCooldownService::validateNotOnCooldown);

        List<YoutubeDataVideoResponse.Item> videoItems = youtubeChannelDataFetchService.fetchVideos(
                target.googleId(),
                extractUploadsPlaylistId(myChannel)
        );
        ChannelDataSyncResult result = youtubeChannelDataPersistenceService.persistChannelData(
                target.userId(),
                myChannel,
                videoItems
        );
        youtubeChannelAnalyticsSyncService.syncAnalytics(target.googleId(), result.channel(), result.videos());
        channelSyncCooldownService.startCooldown(result.channel().getId());
        return buildChannelSyncResponse(result.channel());
    }

    public ChannelSyncResponse refreshChannel(Long channelId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        RefreshChannelTarget target = channelSyncQueryService.getRefreshTarget(userId, channelId);
        channelSyncCooldownService.validateNotOnCooldown(target.channelId());



        YoutubeDataChannelResponse.Item channelItem = youtubeChannelDataFetchService.fetchChannel(target.youtubeChannelId());
        List<YoutubeDataVideoResponse.Item> videoItems = youtubeChannelDataFetchService.fetchVideos(
                target.googleId(),
                extractUploadsPlaylistId(channelItem)
        );

        ChannelDataSyncResult result = youtubeChannelDataPersistenceService.persistChannelData(
                target.userId(),
                channelItem,
                videoItems
        );
        youtubeChannelAnalyticsSyncService.syncAnalytics(target.googleId(), result.channel(), result.videos());
        channelSyncCooldownService.startCooldown(result.channel().getId());
        return buildChannelSyncResponse(result.channel());
    }

    private String extractUploadsPlaylistId(YoutubeDataChannelResponse.Item channelItem) {
        if (channelItem.contentDetails() == null || channelItem.contentDetails().relatedPlaylists() == null) {
            return null;
        }
        return channelItem.contentDetails().relatedPlaylists().uploads();
    }


    private ChannelSyncResponse buildChannelSyncResponse(Channel channel) {
        LocalDateTime updatedAt = channelAnalyticsRepository.findByChannel_Id(channel.getId())
                .map(ChannelAnalytics::getUpdatedAt)
                .orElse(LocalDateTime.now(ZoneOffset.UTC));
        return new ChannelSyncResponse(channel.getId(), channel.getYoutubeChannelId(), updatedAt);
    }
}
