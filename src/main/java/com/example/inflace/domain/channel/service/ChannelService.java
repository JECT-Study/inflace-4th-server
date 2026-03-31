package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse.NewSubscriberVideo;
import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.domain.video.dto.VideoType;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChannelService {

    private final YoutubeDataApiClient youtubeDataApiClient;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;

    @Transactional(readOnly = true)
    public ChannelTopVideosResponse getTopVideos(Long channelId, String contentType) {
        validateChannelExists(channelId);

        VideoType parsedContentType;
        try {
            parsedContentType = VideoType.valueOf(contentType.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        List<Video> videos = videoRepository.findTopVideos(
                channelId,
                parsedContentType.isShort(),
                PageRequest.of(0, 5)
        );

        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        List<ChannelTopVideosResponse.ChannelTopVideo> items = mapTopVideos(videos, videoStatsMap);
        return new ChannelTopVideosResponse(items);
    }

    @Transactional(readOnly = true)
    public ChannelEngagementRateResponse getEngagementRateVideos(Long channelId) {
        validateChannelExists(channelId);

        List<Video> allVideos = videoRepository.findByChannelId(channelId);
        Map<Long, VideoStats> allVideoStatsMap = getVideoStatsMap(allVideos);

        double longFormAverage = calculateAverageEngagementRate(allVideos, allVideoStatsMap, false);
        double shortFormAverage = calculateAverageEngagementRate(allVideos, allVideoStatsMap, true);

        List<ChannelEngagementRateResponse.EngageVideo> items = mapEngagementRateItems(allVideos, allVideoStatsMap);

        return new ChannelEngagementRateResponse(new ChannelEngagementRateResponse.Summary(longFormAverage, shortFormAverage), items);
    }

    @Transactional(readOnly = true)
    public ChannelNewSubscriberResponse getNewSubscriberVideos(Long channelId) {
        validateChannelExists(channelId);

        List<Video> videos = videoRepository.findTopNewSubscriberVideos(
                channelId,
                PageRequest.of(0,5)
        );

        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        List<NewSubscriberVideo> items = new ArrayList<>();

        int rank = 1;
        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());

            items.add(new NewSubscriberVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    videoStats != null ? videoStats.getViewCount() : 0L,
                    videoStats != null ? videoStats.getSubscribersGained() : 0L,
                    videoStats != null && videoStats.getUnsubscribedViewerPercentage() != null
                            ? videoStats.getUnsubscribedViewerPercentage() : 0.0,
                    videoStats != null && videoStats.getAverageViewPercentage() != null
                            ? videoStats.getAverageViewPercentage() : 0.0
            ));
            rank++;
        }

        return new ChannelNewSubscriberResponse(items);
    }

    private void validateChannelExists(Long channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new ApiException(ErrorDefine.CHANNEL_NOT_FOUND);
        }
    }

    private Map<Long, VideoStats> getVideoStatsMap(List<Video> videos) {
        if (videos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> videoIds = new ArrayList<>();
        for (Video video : videos) {
            videoIds.add(video.getId());
        }

        List<VideoStats> videoStatsList = videoStatsRepository.findAllByVideoIds(videoIds);
        Map<Long, VideoStats> videoStatsMap = new HashMap<>();
        for (VideoStats videoStats : videoStatsList) {
            videoStatsMap.put(videoStats.getVideo().getId(), videoStats);
        }

        return videoStatsMap;
    }

    private List<ChannelTopVideosResponse.ChannelTopVideo> mapTopVideos(List<Video> videos, Map<Long, VideoStats> videoStatsMap) {
        List<ChannelTopVideosResponse.ChannelTopVideo> items = new ArrayList<>();
        int rank = 1;

        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());

            items.add(new ChannelTopVideosResponse.ChannelTopVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    videoStats != null ? videoStats.getViewCount() : 0L,
                    video.getRisingScore() != null ? video.getRisingScore() : 0.0,
                    videoStats != null && videoStats.getCtr() != null ? videoStats.getCtr() : 0.0,
                    videoStats != null && videoStats.getAverageViewPercentage() != null
                            ? videoStats.getAverageViewPercentage() : 0.0
            ));

            rank++;
        }

        return items;
    }

    //참여율 도넛차트
    private double calculateAverageEngagementRate(List<Video> videos, Map<Long, VideoStats> videoStatsMap, boolean isShort) {
        long totalLikeCount = 0L;
        long totalCommentCount = 0L;
        long totalViewCount = 0L;

        for (Video video : videos) {
            if (video.isShort() != isShort) {
                continue;
            }

            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null) {
                continue;
            }

            totalLikeCount += videoStats.getLikeCount() != null ? videoStats.getLikeCount() : 0L;
            totalCommentCount += videoStats.getCommentCount() != null ? videoStats.getCommentCount() : 0L;
            totalViewCount += videoStats.getViewCount() != null ? videoStats.getViewCount() : 0L;
        }

        if (totalViewCount == 0L) {
            return 0.0;
        }

        return AnalyticsCalculator.engagementRate(totalLikeCount, totalCommentCount, totalViewCount);
    }

    //참여율 항목 만들기
    private List<ChannelEngagementRateResponse.EngageVideo> mapEngagementRateItems(List<Video> videos, Map<Long, VideoStats> videoStatsMap) {
        List<ChannelEngagementRateResponse.EngageVideo> items = new ArrayList<>();

        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null) {
                continue;
            }

            items.add(new ChannelEngagementRateResponse.EngageVideo(
                    0,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    video.isShort() ? VideoType.SHORT_FORM.name() : VideoType.LONG_FORM.name(),
                    AnalyticsCalculator.engagementRate(
                            videoStats.getLikeCount(),
                            videoStats.getCommentCount(),
                            videoStats.getViewCount()
                    )
            ));
        }

        items.sort((item1, item2) -> {
            int compareEngagementRate = Double.compare(item2.engagementRate(), item1.engagementRate());
            if (compareEngagementRate != 0) {
                return compareEngagementRate;
            }
            return Long.compare(item2.videoId(), item1.videoId());
        });

        List<ChannelEngagementRateResponse.EngageVideo> rankedItems = new ArrayList<>();
        int rank = 1;
        for (ChannelEngagementRateResponse.EngageVideo item : items) {
            if (rank > 5) {
                break;
            }

            rankedItems.add(new ChannelEngagementRateResponse.EngageVideo(
                    rank,
                    item.videoId(),
                    item.title(),
                    item.thumbnailUrl(),
                    item.contentType(),
                    item.engagementRate()
            ));
            rank++;
        }

        return rankedItems;
    }

    private YoutubeDataChannelResponse getYoutubeChannel(String channelId, String parts) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId, parts);
        return response;
    }
}
