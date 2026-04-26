package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.dto.ChannelTopMainVideosResponse;
import com.example.inflace.domain.channel.domain.SubscriberLog;
import com.example.inflace.domain.channel.dto.ChannelEngagementRateResponse;
import com.example.inflace.domain.channel.dto.ChannelKpiResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse;
import com.example.inflace.domain.channel.dto.ChannelNewSubscriberResponse.NewSubscriberVideo;
import com.example.inflace.domain.channel.dto.ChannelSubscriberDistributionResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberPatternResponse;
import com.example.inflace.domain.channel.dto.ChannelSubscriberTrendResponse;
import com.example.inflace.domain.channel.dto.ChannelVideoSliceResult;
import com.example.inflace.domain.channel.dto.enums.ChannelSubscriberTrendRange;
import com.example.inflace.domain.channel.dto.enums.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.enums.ChannelVideoSort;
import com.example.inflace.domain.channel.dto.ChannelVideosRequest;
import com.example.inflace.domain.channel.dto.ChannelVideosResponse;
import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelAnalyticsRepository;
import com.example.inflace.domain.channel.repository.SubscriberLogRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoAnalytics;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.channel.dto.ChannelTopVideosResponse;
import com.example.inflace.domain.video.dto.VideoType;
import com.example.inflace.domain.video.repository.VideoQueryRepository;
import com.example.inflace.domain.video.repository.VideoAnalyticsRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Limit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeDataApiClient youtubeDataApiClient;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final VideoQueryRepository videoQueryRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final ChannelAnalyticsRepository channelAnalyticsRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final SubscriberLogRepository subscriberLogRepository;

    @ReadOnlyTransactional
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
        Map<Long, VideoAnalytics> videoAnalyticsMap = getVideoAnalyticsMap(videos);
        List<ChannelTopVideosResponse.ChannelTopVideo> items = mapTopVideos(videos, videoStatsMap, videoAnalyticsMap);
        return new ChannelTopVideosResponse(items);
    }

    @ReadOnlyTransactional
    public ChannelEngagementRateResponse getEngagementRateVideos(Long channelId) {
        validateChannelExists(channelId);

        List<Video> allVideos = videoRepository.findByChannelId(channelId);
        Map<Long, VideoStats> allVideoStatsMap = getVideoStatsMap(allVideos);

        double longFormAverage = calculateAverageEngagementRate(allVideos, allVideoStatsMap, false);
        double shortFormAverage = calculateAverageEngagementRate(allVideos, allVideoStatsMap, true);

        List<ChannelEngagementRateResponse.EngageVideo> items = mapEngagementRateItems(allVideos, allVideoStatsMap);

        return new ChannelEngagementRateResponse(new ChannelEngagementRateResponse.Summary(longFormAverage, shortFormAverage), items);
    }

    @ReadOnlyTransactional
    public ChannelNewSubscriberResponse getNewSubscriberVideos(Long channelId) {
        validateChannelExists(channelId);

        List<Video> videos = videoRepository.findTopNewSubscriberVideos(
                channelId,
                PageRequest.of(0,5)
        );

        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        Map<Long, VideoAnalytics> videoAnalyticsMap = getVideoAnalyticsMap(videos);
        List<NewSubscriberVideo> items = new ArrayList<>();

        int rank = 1;
        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            VideoAnalytics videoAnalytics = videoAnalyticsMap.get(video.getId());
            items.add(ChannelNewSubscriberResponse.NewSubscriberVideo.from(rank, video, videoStats, videoAnalytics));
            rank++;
        }

        return new ChannelNewSubscriberResponse(items);
    }

    @ReadOnlyTransactional
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
                calculateAverageRetentionRate(videos, getVideoAnalyticsMap(videos)),
                weeklyUploadCount
        );
    }

    @ReadOnlyTransactional
    public ChannelTopMainVideosResponse getMainTopVideos(Long channelId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        validateChannelOwnership(channel, userId);

        List<Video> videos = videoRepository.findAllTopVideos(channelId, Limit.of(5));
        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        Map<Long, VideoAnalytics> videoAnalyticsMap = getVideoAnalyticsMap(videos);
        List<ChannelTopMainVideosResponse.ChannelTopMainVideo> items = new ArrayList<>();
        long rank = 1;

        for (Video video : videos) {
            items.add(ChannelTopMainVideosResponse.ChannelTopMainVideo.of(
                    rank,
                    video,
                    videoStatsMap.get(video.getId()),
                    videoAnalyticsMap.get(video.getId())
            ));
            rank++;
        }

        return new ChannelTopMainVideosResponse(items);
    }

    private void validateChannelOwnership(Channel channel, UUID userId) {
        if (!channel.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }
    }

    public ChannelSubscriberPatternResponse getSubscriberPattern(Long channelId) {
        validateChannelExists(channelId);

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));
        ChannelAnalytics channelAnalytics = channelAnalyticsRepository.findTopByChannel_IdOrderByEndDateDesc(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        return ChannelSubscriberPatternResponse.from(
                channelStats.getTotalViewCount(),
                channelAnalytics.getSubscriberViewCount()
        );
    }

    @ReadOnlyTransactional
    public ChannelSubscriberDistributionResponse getSubscriberDistribution(Long channelId) {
        validateChannelExists(channelId);

        ChannelAnalytics channelAnalytics = channelAnalyticsRepository.findTopByChannel_IdOrderByEndDateDesc(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        return ChannelSubscriberDistributionResponse.from(
                channelAnalytics.getAudienceGender(),
                channelAnalytics.getAudienceAge(),
                channelAnalytics.getAudienceCountry()
        );
    }


    @ReadOnlyTransactional
    public ChannelVideosResponse getChannelVideos(
            Long channelId,
            String keyword,
            String sort,
            String format,
            Boolean isAd,
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

        ChannelVideoFormat parsedFormat;
        try {
            parsedFormat = ChannelVideoFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        ChannelVideosRequest request = new ChannelVideosRequest(keyword, parsedSort, parsedFormat, isAd, cursor, size);
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

    @ReadOnlyTransactional
    public ChannelSubscriberTrendResponse getSubscriberTrend(Long channelId, String rangeValue) {
        validateChannelExists(channelId);

        ChannelSubscriberTrendRange range = ChannelSubscriberTrendRange.from(rangeValue);
        SubscriberLog latestHistory = subscriberLogRepository
                .findTopByChannel_IdOrderByRecordedDateDesc(channelId)
                .orElse(null);

        if (latestHistory == null) {
            return new ChannelSubscriberTrendResponse(range.value(), List.of());
        }

        LocalDate endDate = latestHistory.getRecordedDate();
        LocalDate startDate = range.startDate(endDate);

        List<SubscriberLog> histories = new ArrayList<>(subscriberLogRepository.findLogsInRange(
                channelId,
                startDate,
                endDate
        ));

        subscriberLogRepository
                .findTopByChannel_IdAndRecordedDateBeforeOrderByRecordedDateDesc(channelId, startDate)
                .ifPresent(histories::add);

        List<ChannelSubscriberTrendResponse.Point> points = buildSubscriberTrendPoints(histories, range, startDate, endDate);
        return new ChannelSubscriberTrendResponse(range.value(), points);
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

        List<VideoStats> videoStatsList = videoStatsRepository.findAllByVideoIdIn(videoIds);
        Map<Long, VideoStats> videoStatsMap = new HashMap<>();
        for (VideoStats videoStats : videoStatsList) {
            videoStatsMap.put(videoStats.getVideo().getId(), videoStats);
        }

        return videoStatsMap;
    }

    private Map<Long, VideoAnalytics> getVideoAnalyticsMap(List<Video> videos) {
        if (videos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> videoIds = new ArrayList<>();
        for (Video video : videos) {
            videoIds.add(video.getId());
        }

        List<VideoAnalytics> videoAnalyticsList = videoAnalyticsRepository.findAllByVideoIdIn(videoIds);
        Map<Long, VideoAnalytics> videoAnalyticsMap = new HashMap<>();
        for (VideoAnalytics videoAnalytics : videoAnalyticsList) {
            videoAnalyticsMap.put(videoAnalytics.getVideo().getId(), videoAnalytics);
        }

        return videoAnalyticsMap;
    }

    private List<ChannelTopVideosResponse.ChannelTopVideo> mapTopVideos(
            List<Video> videos,
            Map<Long, VideoStats> videoStatsMap,
            Map<Long, VideoAnalytics> videoAnalyticsMap
    ) {
        List<ChannelTopVideosResponse.ChannelTopVideo> items = new ArrayList<>();
        int rank = 1;

        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            VideoAnalytics videoAnalytics = videoAnalyticsMap.get(video.getId());
            items.add(ChannelTopVideosResponse.ChannelTopVideo.from(rank, video, videoStats, videoAnalytics));
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
    private double calculateAverageRetentionRate(List<Video> videos, Map<Long, VideoAnalytics> videoAnalyticsMap) {
        double totalAverageViewPercentage = 0.0;
        int count = 0;

        for (Video video : videos) {
            VideoAnalytics videoAnalytics = videoAnalyticsMap.get(video.getId());
            if (videoAnalytics == null || videoAnalytics.getAverageViewPercentage() == null) {
                continue;
            }

            totalAverageViewPercentage += videoAnalytics.getAverageViewPercentage();
            count++;
        }

        return count == 0 ? 0.0 : totalAverageViewPercentage / count;
    }

    //하루 1건 저장을 전제로 조회된 히스토리를 LocalDate -> subscriberCount 형태의 정렬된 맵으로 만든다.
    private List<ChannelSubscriberTrendResponse.Point> buildSubscriberTrendPoints(
            List<SubscriberLog> histories,
            ChannelSubscriberTrendRange range,
            LocalDate startDate,
            LocalDate endDate
    ) {
        NavigableMap<LocalDate, Long> subscriberCountByDate = new TreeMap<>();
        for (SubscriberLog history : histories) {
            subscriberCountByDate.put(history.getRecordedDate(), history.getSubscriberCount());
        }

        List<LocalDate> pointDates = createPointDates(range, startDate, endDate);
        List<ChannelSubscriberTrendResponse.Point> points = new ArrayList<>();
        for (LocalDate pointDate : pointDates) {
            Map.Entry<LocalDate, Long> floorEntry = subscriberCountByDate.floorEntry(pointDate);
            Long subscriberCount = floorEntry != null ? floorEntry.getValue() : 0L;
            points.add(new ChannelSubscriberTrendResponse.Point(pointDate.toString(), subscriberCount));
        }

        return points;
    }

    //x축 포인트 6개를 만든다.
    private List<LocalDate> createPointDates(
            ChannelSubscriberTrendRange range,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (range == ChannelSubscriberTrendRange.DAYS_7) {
            return List.of(
                    startDate,
                    startDate.plusDays(1),
                    startDate.plusDays(2),
                    startDate.plusDays(3),
                    startDate.plusDays(4),
                    endDate
            );
        }

        if (range == ChannelSubscriberTrendRange.DAYS_30) {
            return createFixedIntervalPointDates(endDate, 5);
        }

        if (range == ChannelSubscriberTrendRange.DAYS_90) {
            return createFixedIntervalPointDates(endDate, 15);
        }

        if (range == ChannelSubscriberTrendRange.DAYS_180) {
            return createFixedIntervalPointDates(endDate, 30);
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            long offset = Math.round((double) totalDays * i / 5);
            dates.add(startDate.plusDays(offset));
        }

        return dates;
    }

    // 마지막 수집일을 기준으로 고정 일수 간격을 거꾸로 적용해 6개 포인트를 만든다.
    private List<LocalDate> createFixedIntervalPointDates(LocalDate endDate, int intervalDays) {
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            dates.add(endDate.minusDays((long) i * intervalDays));
        }

        return dates;
    }

    private YoutubeDataChannelResponse getYoutubeChannel(String channelId, String parts) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId, parts);
        return response;
    }
}
