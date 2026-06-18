package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.AnalyticsSyncContext;
import com.example.inflace.domain.channel.dto.YoutubeAnalyticsSyncData;
import com.example.inflace.domain.video.domain.Video;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YoutubeChannelAnalyticsSyncService {

    private final YoutubeChannelAnalyticsQueryService youtubeChannelAnalyticsQueryService;
    private final YoutubeChannelAnalyticsFetchService youtubeChannelAnalyticsFetchService;
    private final YoutubeChannelAnalyticsPersistenceService youtubeChannelAnalyticsPersistenceService;

    public void syncAnalytics(String googleId, Channel channel, List<Video> videos) {
        AnalyticsSyncContext context = youtubeChannelAnalyticsQueryService.loadContext(channel.getId(), videos);
        YoutubeAnalyticsSyncData data = youtubeChannelAnalyticsFetchService.fetchAnalytics(googleId, context);
        youtubeChannelAnalyticsPersistenceService.persistAnalytics(context.channelId(), data);
    }
}
