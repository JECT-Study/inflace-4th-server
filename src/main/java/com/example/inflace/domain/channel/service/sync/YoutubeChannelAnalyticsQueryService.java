package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.domain.SubscriberLog;
import com.example.inflace.domain.channel.dto.AnalyticsSyncContext;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.channel.repository.SubscriberLogRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeChannelAnalyticsQueryService {

    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final SubscriberLogRepository subscriberLogRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final VideoStatsRepository videoStatsRepository;

    @Transactional(readOnly = true)
    public AnalyticsSyncContext loadContext(Long channelId, List<Video> videos) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        Long subscriberCount = channelStatsRepository.findByChannel_Id(channelId)
                .map(ChannelStats::getSubscriberCount)
                .orElse(null);
        LocalDate latestSubscriberLogDate = subscriberLogRepository.findTopByChannel_IdOrderByRecordedDateDesc(channelId)
                .map(SubscriberLog::getRecordedDate)
                .orElse(null);

        List<Long> videoIds = videos.stream()
                .map(Video::getId)
                .toList();
        Set<Long> existingVideoAnalyticsIds = videoIds.isEmpty()
                ? Set.of()
                : videoAnalyticsRepository.findAllByVideoIdIn(videoIds).stream()
                .map(VideoAnalytics::getVideo)
                .map(Video::getId)
                .collect(Collectors.toSet());
        Map<Long, VideoStats> videoStatsByVideoId = videoIds.isEmpty()
                ? Map.of()
                : videoStatsRepository.findAllByVideoIdIn(videoIds).stream()
                .collect(Collectors.toMap(
                        videoStats -> videoStats.getVideo().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<AnalyticsSyncContext.VideoContext> videoContexts = videos.stream()
                .map(video -> new AnalyticsSyncContext.VideoContext(
                        video.getId(),
                        video.getYoutubeVideoId(),
                        video.getPublishedAt(),
                        existingVideoAnalyticsIds.contains(video.getId()),
                        videoStatsByVideoId.containsKey(video.getId())
                                ? videoStatsByVideoId.get(video.getId()).getViewCount()
                                : null
                ))
                .toList();

        return new AnalyticsSyncContext(
                channel.getId(),
                channel.getYoutubeChannelId(),
                channel.getYoutubePublishedAt(),
                subscriberCount,
                latestSubscriberLogDate,
                videoContexts
        );
    }
}
