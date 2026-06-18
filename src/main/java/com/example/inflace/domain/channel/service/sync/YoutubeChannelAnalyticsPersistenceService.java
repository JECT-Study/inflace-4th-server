package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import com.example.inflace.domain.channel.domain.SubscriberLog;
import com.example.inflace.domain.channel.dto.YoutubeAnalyticsSyncData;
import com.example.inflace.domain.channel.dto.YoutubeAnalyticsSyncData.SubscriberLogData;
import com.example.inflace.domain.channel.repository.ChannelAnalyticsRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.SubscriberLogRepository;
import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeChannelAnalyticsPersistenceService {

    private final ChannelRepository channelRepository;
    private final ChannelAnalyticsRepository channelAnalyticsRepository;
    private final SubscriberLogRepository subscriberLogRepository;
    private final VideoRepository videoRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final AudienceRetentionRepository audienceRetentionRepository;

    @Transactional
    public void persistAnalytics(Long channelId, YoutubeAnalyticsSyncData data) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        persistChannelAnalytics(channel, data.channelAnalytics());
        persistSubscriberLogs(channel, data.subscriberLogs());
        persistVideoAnalytics(data.videoAnalytics());
        persistAudienceRetentions(data.audienceRetentions());
    }

    private void persistChannelAnalytics(
            Channel channel,
            YoutubeAnalyticsSyncData.ChannelAnalyticsData data
    ) {
        if (!data.update()) {
            return;
        }

        channelAnalyticsRepository.findByChannel_Id(channel.getId())
                .ifPresentOrElse(
                        analytics -> analytics.update(
                                data.startDate(),
                                data.endDate(),
                                data.collectedAt(),
                                data.views(),
                                data.subscriberViewCount(),
                                data.nonSubscriberViewCount(),
                                data.watchedMinutes(),
                                data.averageViewDurationSeconds(),
                                data.audienceGender(),
                                data.audienceAge(),
                                data.audienceCountry()
                        ),
                        () -> channelAnalyticsRepository.save(ChannelAnalytics.builder()
                                .channel(channel)
                                .startDate(data.startDate())
                                .endDate(data.endDate())
                                .collectedAt(data.collectedAt())
                                .views(data.views())
                                .subscriberViewCount(data.subscriberViewCount())
                                .nonSubscriberViewCount(data.nonSubscriberViewCount())
                                .watchedMinutes(data.watchedMinutes())
                                .averageViewDurationSeconds(data.averageViewDurationSeconds())
                                .audienceGender(data.audienceGender())
                                .audienceAge(data.audienceAge())
                                .audienceCountry(data.audienceCountry())
                                .build())
                );
    }

    private void persistSubscriberLogs(
            Channel channel,
            List<SubscriberLogData> logs
    ) {
        if (logs.isEmpty()) {
            return;
        }

        subscriberLogRepository.saveAll(logs.stream()
                .map(log -> SubscriberLog.builder()
                        .channel(channel)
                        .subscriberCount(log.subscriberCount())
                        .subscribersGained(log.subscribersGained())
                        .subscribersLost(log.subscribersLost())
                        .recordedDate(log.recordedDate())
                        .build())
                .toList());
    }

    private void persistVideoAnalytics(List<YoutubeAnalyticsSyncData.VideoAnalyticsData> videoAnalyticsData) {
        for (YoutubeAnalyticsSyncData.VideoAnalyticsData data : videoAnalyticsData) {
            Video video = videoRepository.findById(data.videoId())
                    .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));
            VideoAnalytics analytics = videoAnalyticsRepository.findByVideoId(data.videoId())
                    .orElseGet(() -> VideoAnalytics.builder().video(video).build());

            if (data.updateSummary()) {
                analytics.update(
                        data.shareCount(),
                        data.subscribersGained(),
                        data.ctr(),
                        data.avgWatchDuration(),
                        data.averageViewPercentage(),
                        data.collectedAt()
                );
                if (analytics.getId() == null) {
                    videoAnalyticsRepository.save(analytics);
                }
            }

            if (data.unsubscribedViewCount() != null || data.unsubscribedViewerPercentage() != null) {
                analytics.updateUnsubscribed(data.unsubscribedViewCount(), data.unsubscribedViewerPercentage());
            }
        }
    }

    private void persistAudienceRetentions(List<YoutubeAnalyticsSyncData.AudienceRetentionData> retentionsData) {
        for (YoutubeAnalyticsSyncData.AudienceRetentionData data : retentionsData) {
            Video video = videoRepository.findById(data.videoId())
                    .orElseThrow(() -> new ApiException(ErrorDefine.VIDEO_NOT_FOUND));

            audienceRetentionRepository.deleteByVideoId(data.videoId());
            List<AudienceRetention> retentions = data.retentions().stream()
                    .map(retention -> AudienceRetention.builder()
                            .video(video)
                            .timeRatio(retention.timeRatio())
                            .retentionRate(retention.retentionRate())
                            .collectedAt(retention.collectedAt())
                            .build())
                    .toList();
            audienceRetentionRepository.saveAll(retentions);

            videoAnalyticsRepository.findByVideoId(data.videoId())
                    .ifPresent(analytics -> analytics.updateRelativeRetention(data.relativeRetentionPerformance()));
        }
    }
}
