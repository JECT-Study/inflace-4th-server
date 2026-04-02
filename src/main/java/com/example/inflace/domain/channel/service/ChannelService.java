package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse.NewSubscriberVideo;
import com.example.inflace.domain.channel.dto.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.ChannelVideoSliceResult;
import com.example.inflace.domain.channel.dto.ChannelVideoSort;
import com.example.inflace.domain.channel.dto.ChannelVideosRequest;
import com.example.inflace.domain.channel.dto.ChannelVideosResponse;
import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.domain.video.dto.VideoType;
import com.example.inflace.domain.video.repository.VideoQueryRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.time.LocalDateTime;
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
    private final VideoQueryRepository videoQueryRepository;
    private final ChannelStatsRepository channelStatsRepository;
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
            items.add(ChannelNewSubscriberResponse.NewSubscriberVideo.from(rank, video, videoStats));
            rank++;
        }

        return new ChannelNewSubscriberResponse(items);
    }

    @Transactional(readOnly = true)
    public ChannelKpiResponse getChannelKpi(Long channelId) {
        validateChannelExists(channelId);

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        List<Video> videos = videoRepository.findByChannelId(channelId);
        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        Long recentUploadCount = videoRepository.countByChannelIdAndPublishedAtGreaterThanEqual(channelId, oneMonthAgo);
        double weeklyUploadCount = Math.round((recentUploadCount / (30.0 / 7.0)) * 100) / 100.0;

        return ChannelKpiResponse.from(
                channelStats.getTotalViewCount(),
                channelStats.getAvgEngagementRate(),
                calculateAverageRetentionRate(videos, videoStatsMap),
                weeklyUploadCount
        );
    }

    @Transactional(readOnly = true)
    public ChannelSubscriberPatternResponse getSubscriberPattern(Long channelId) {
        validateChannelExists(channelId);

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        return ChannelSubscriberPatternResponse.from(
                channelStats.getTotalViewCount(),
                channelStats.getSubscriberViewCount()
        );
    }

    @Transactional(readOnly = true)
    public ChannelSubscriberDistributionResponse getSubscriberDistribution(Long channelId) {
        validateChannelExists(channelId);

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        return ChannelSubscriberDistributionResponse.from(channelStats);
    }

    @Transactional(readOnly = true)
    public ChannelVideosResponse getChannelVideos(
            Long channelId,
            String keyword,
            String sort,
            String cursor,
            Integer size
    ) {
        validateChannelExists(channelId);

        ChannelVideoSort parsedSort;
        try {
            parsedSort = ChannelVideoSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        ChannelVideosRequest request = new ChannelVideosRequest(keyword, parsedSort, cursor, size);
        ChannelVideoSliceResult result = videoQueryRepository.findChannelVideos(channelId, request);

        return new ChannelVideosResponse(
                result.videos(),
                new ChannelVideosResponse.PageInfo(
                        request.size(),
                        result.nextCursor(),
                        result.hasNext()
                )
        );
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
            items.add(ChannelTopVideosResponse.ChannelTopVideo.from(rank, video, videoStats));
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
        List<EngagementRateItem> items = new ArrayList<>();

        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null) {
                continue;
            }

            items.add(new EngagementRateItem(
                    video,
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
            return Long.compare(item2.video().getId(), item1.video().getId());
        });

        List<ChannelEngagementRateResponse.EngageVideo> rankedItems = new ArrayList<>();
        int rank = 1;
        for (EngagementRateItem item : items) {
            if (rank > 5) {
                break;
            }

            rankedItems.add(ChannelEngagementRateResponse.EngageVideo.from(
                    rank,
                    item.video(),
                    item.engagementRate()
            ));
            rank++;
        }

        return rankedItems;
    }

    private record EngagementRateItem(
            Video video,
            double engagementRate
    ) {
    }

    //채널 참여율 평균 구하기
    private double calculateAverageRetentionRate(List<Video> videos, Map<Long, VideoStats> videoStatsMap) {
        double totalAverageViewPercentage = 0.0;
        int count = 0;

        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null || videoStats.getAverageViewPercentage() == null) {
                continue;
            }

            totalAverageViewPercentage += videoStats.getAverageViewPercentage();
            count++;
        }

        return count == 0 ? 0.0 : totalAverageViewPercentage / count;
    }

    private YoutubeDataChannelResponse getYoutubeChannel(String channelId, String parts) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId, parts);
        return response;
    }
}
