package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelCategory;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.dto.ChannelDataSyncResult;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelCategoryRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.domain.VideoTag;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.domain.video.repository.VideoTagRepository;
import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;
import com.example.inflace.domain.youtubecategory.repository.YoutubeCategoryRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.util.AnalyticsCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class YoutubeChannelDataPersistenceService {

    private static final int RECENT_VIDEO_SAMPLE_SIZE = 30;
    private static final int CHANNEL_CATEGORY_LIMIT = 3;
    private static final int SHORTS_MAX_DURATION_SECONDS = 180;

    private final UserReadRepository userReadRepository;
    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository channelCategoryRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final YoutubeCategoryRepository youtubeCategoryRepository;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoTagRepository videoTagRepository;

    @Transactional
    public ChannelDataSyncResult persistChannelData(
            UUID userId,
            YoutubeDataChannelResponse.Item channelItem,
            List<YoutubeDataVideoResponse.Item> videoItems
    ) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        Channel channel = upsertChannel(user, channelItem);
        upsertChannelStats(channel, channelItem.statistics());
        syncChannelVideos(channel, channelItem.statistics(), videoItems);
        refreshCalculatedChannelStats(channel);
        refreshVideoRisingScores(channel);
        refreshChannelCategories(channel);

        return new ChannelDataSyncResult(channel, videoRepository.findByChannelId(channel.getId()));
    }

    private Channel upsertChannel(User user, YoutubeDataChannelResponse.Item item) {
        return channelRepository.findByUser_IdAndYoutubeChannelId(user.getId(), item.id())
                .map(channel -> updateChannel(channel, item))
                .orElseGet(() -> channelRepository.findByYoutubeChannelIdAndUserIsNull(item.id())
                        .map(channel -> {
                            channel.updateUser(user);
                            return updateChannel(channel, item);
                        })
                        .orElseGet(() -> channelRepository.save(Channel.builder()
                                .user(user)
                                .name(item.snippet() == null ? null : item.snippet().title())
                                .youtubeChannelId(item.id())
                                .channelHandle(item.snippet() == null ? null : item.snippet().customUrl())
                                .profileImageUrl(extractChannelThumbnailUrl(item.snippet() == null ? null : item.snippet().thumbnails()))
                                .uploadsPlaylistId(extractUploadsPlaylistId(item.contentDetails()))
                                .youtubePublishedAt(parsePublishedAt(item.snippet() == null ? null : item.snippet().publishedAt()))
                                .build())));
    }

    private Channel updateChannel(Channel channel, YoutubeDataChannelResponse.Item item) {
        channel.update(
                item.snippet() == null ? null : item.snippet().title(),
                item.snippet() == null ? null : item.snippet().customUrl(),
                extractChannelThumbnailUrl(item.snippet() == null ? null : item.snippet().thumbnails()),
                extractUploadsPlaylistId(item.contentDetails()),
                parsePublishedAt(item.snippet() == null ? null : item.snippet().publishedAt())
        );
        return channel;
    }

    private void upsertChannelStats(Channel channel, YoutubeDataChannelResponse.Statistics statistics) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Long subscriberCount = statistics == null ? null : toLong(statistics.subscriberCount());
        Long totalViewCount = statistics == null ? null : toLong(statistics.viewCount());
        Long totalVideoCount = statistics == null ? null : toLong(statistics.videoCount());

        channelStatsRepository.findByChannel_Id(channel.getId())
                .ifPresentOrElse(
                        channelStats -> channelStats.update(subscriberCount, totalViewCount, totalVideoCount, now),
                        () -> channelStatsRepository.save(ChannelStats.builder()
                                .channel(channel)
                                .subscriberCount(subscriberCount)
                                .totalViewCount(totalViewCount)
                                .totalVideoCount(totalVideoCount)
                                .collectedAt(now)
                                .build())
                );
    }

    private void syncChannelVideos(
            Channel channel,
            YoutubeDataChannelResponse.Statistics channelStatistics,
            List<YoutubeDataVideoResponse.Item> videoItems
    ) {
        if (!StringUtils.hasText(channel.getUploadsPlaylistId())) {
            return;
        }

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channel.getId())
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        for (YoutubeDataVideoResponse.Item item : videoItems) {
            if (item == null || !StringUtils.hasText(item.id())) {
                continue;
            }

            Video video = upsertVideo(channel, item);
            upsertVideoStats(video, item, channelStats, channelStatistics);
            syncVideoTags(video, item);
        }
    }

    private Video upsertVideo(Channel channel, YoutubeDataVideoResponse.Item item) {
        Integer durationSeconds = parseDurationSeconds(item.contentDetails());
        boolean isShort = durationSeconds != null && durationSeconds <= SHORTS_MAX_DURATION_SECONDS;
        boolean isAdvertisement = hasPaidProductPlacement(item);

        return videoRepository.findByYoutubeVideoId(item.id())
                .map(video -> updateVideo(video, item, durationSeconds, isShort, isAdvertisement))
                .orElseGet(() -> videoRepository.save(Video.builder()
                        .channel(channel)
                        .categoryId(toInteger(item.snippet() == null ? null : item.snippet().categoryId()))
                        .youtubeVideoId(item.id())
                        .title(item.snippet() == null ? null : item.snippet().title())
                        .description(item.snippet() == null ? null : item.snippet().description())
                        .thumbnailUrl(extractVideoThumbnailUrl(item.snippet() == null ? null : item.snippet().thumbnails()))
                        .durationSeconds(durationSeconds)
                        .isShort(isShort)
                        .isAdvertisement(isAdvertisement)
                        .publishedAt(parsePublishedAt(item.snippet() == null ? null : item.snippet().publishedAt()))
                        .build()));
    }

    private Video updateVideo(
            Video video,
            YoutubeDataVideoResponse.Item item,
            Integer durationSeconds,
            boolean isShort,
            boolean isAdvertisement
    ) {
        video.update(
                toInteger(item.snippet() == null ? null : item.snippet().categoryId()),
                item.snippet() == null ? null : item.snippet().title(),
                item.snippet() == null ? null : item.snippet().description(),
                extractVideoThumbnailUrl(item.snippet() == null ? null : item.snippet().thumbnails()),
                durationSeconds,
                isShort,
                isAdvertisement,
                parsePublishedAt(item.snippet() == null ? null : item.snippet().publishedAt())
        );
        return video;
    }

    private void upsertVideoStats(
            Video video,
            YoutubeDataVideoResponse.Item item,
            ChannelStats channelStats,
            YoutubeDataChannelResponse.Statistics channelStatistics
    ) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        YoutubeDataVideoResponse.Statistics statistics = item.statistics();
        Long viewCount = statistics == null ? null : toLong(statistics.viewCount());
        Long likeCount = statistics == null ? null : toLong(statistics.likeCount());
        Long commentCount = statistics == null ? null : toLong(statistics.commentCount());
        Double vph = calculateViewsPerHour(viewCount, video.getPublishedAt(), now);
        Double outlierScore = calculateOutlierScore(viewCount, channelStats, channelStatistics);

        videoStatsRepository.findByVideoId(video.getId())
                .ifPresentOrElse(
                        videoStats -> videoStats.update(
                                viewCount,
                                likeCount,
                                commentCount,
                                vph,
                                outlierScore,
                                null,
                                now
                        ),
                        () -> videoStatsRepository.save(VideoStats.builder()
                                .video(video)
                                .viewCount(viewCount)
                                .likeCount(likeCount)
                                .commentCount(commentCount)
                                .vph(vph)
                                .outlierScore(outlierScore)
                                .risingScore(null)
                                .collectedAt(now)
                                .build())
                );
    }

    private void syncVideoTags(Video video, YoutubeDataVideoResponse.Item item) {
        List<String> incomingTags = item.snippet() == null || item.snippet().tags() == null
                ? List.of()
                : item.snippet().tags().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        List<VideoTag> existingTags = videoTagRepository.findAllByVideoId(video.getId());
        Map<String, VideoTag> existingByTag = existingTags.stream()
                .collect(Collectors.toMap(VideoTag::getTag, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<VideoTag> toDelete = existingTags.stream()
                .filter(tag -> !incomingTags.contains(tag.getTag()))
                .toList();
        if (!toDelete.isEmpty()) {
            videoTagRepository.deleteAll(toDelete);
        }

        List<VideoTag> toCreate = incomingTags.stream()
                .filter(tag -> !existingByTag.containsKey(tag))
                .map(tag -> VideoTag.of(video, tag))
                .toList();
        if (!toCreate.isEmpty()) {
            videoTagRepository.saveAll(toCreate);
        }
    }

    private void refreshCalculatedChannelStats(Channel channel) {
        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channel.getId())
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        List<Video> videos = videoRepository.findByChannelId(channel.getId());
        if (videos.isEmpty()) {
            channelStats.updateCalculatedMetrics(0, 0.0, 0.0, 0.0, LocalDateTime.now(ZoneOffset.UTC));
            return;
        }

        Map<Long, VideoStats> videoStatsMap = videoStatsRepository.findAllByVideoIdIn(
                        videos.stream().map(Video::getId).toList()
                ).stream()
                .collect(Collectors.toMap(videoStats -> videoStats.getVideo().getId(), Function.identity()));

        LocalDateTime thirtyDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);
        int recentUploadCount30d = (int) videos.stream()
                .filter(video -> video.getPublishedAt() != null && !video.getPublishedAt().isBefore(thirtyDaysAgo))
                .count();

        List<Video> recentVideos = videos.stream()
                .filter(video -> video.getPublishedAt() != null && !video.getPublishedAt().isBefore(thirtyDaysAgo))
                .sorted(Comparator.comparing(Video::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long totalViews = 0;
        long totalLikes = 0;
        long totalComments = 0;
        for (Video video : recentVideos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null) {
                continue;
            }
            totalViews += defaultLong(videoStats.getViewCount());
            totalLikes += defaultLong(videoStats.getLikeCount());
            totalComments += defaultLong(videoStats.getCommentCount());
        }

        double avgViewsRecentN = recentVideos.isEmpty()
                ? 0.0
                : round((double) totalViews / recentVideos.size(), 2);
        double avgEngagementRateRecentN = totalViews > 0
                ? round(((double) (totalLikes + totalComments) / totalViews) * 100.0, 2)
                : 0.0;

        double avgOutlierScoreRecentExcludingTop5Pct =
                calculateAverageOutlierScoreRecentExcludingTop5Pct(recentVideos, videoStatsMap, channelStats);

        channelStats.updateCalculatedMetrics(
                recentUploadCount30d,
                avgViewsRecentN,
                avgEngagementRateRecentN,
                avgOutlierScoreRecentExcludingTop5Pct,
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }

    private void refreshVideoRisingScores(Channel channel) {
        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channel.getId())
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));
        Double avgEngagementRate = channelStats.getAvgEngagementRate();
        if (avgEngagementRate == null || avgEngagementRate <= 0) {
            return;
        }

        List<Video> videos = videoRepository.findByChannelId(channel.getId());
        if (videos.isEmpty()) {
            return;
        }

        Map<Long, VideoStats> videoStatsMap = videoStatsRepository.findAllByVideoIdIn(
                        videos.stream().map(Video::getId).toList()
                ).stream()
                .collect(Collectors.toMap(videoStats -> videoStats.getVideo().getId(), Function.identity()));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Video video : videos) {
            VideoStats videoStats = videoStatsMap.get(video.getId());
            if (videoStats == null) {
                continue;
            }

            Double engagementRate = calculateEngagementRate(
                    videoStats.getLikeCount(),
                    videoStats.getCommentCount(),
                    videoStats.getViewCount()
            );
            Double risingScore = calculateRisingScore(
                    engagementRate,
                    avgEngagementRate,
                    videoStats.getOutlierScore()
            );

            videoStats.update(
                    videoStats.getViewCount(),
                    videoStats.getLikeCount(),
                    videoStats.getCommentCount(),
                    videoStats.getVph(),
                    videoStats.getOutlierScore(),
                    risingScore,
                    now
            );
        }
    }

    private void refreshChannelCategories(Channel channel) {
        List<Video> recentVideos = videoRepository.findByChannelId(channel.getId()).stream()
                .sorted(Comparator.comparing(Video::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_VIDEO_SAMPLE_SIZE)
                .toList();
        if (recentVideos.isEmpty()) {
            deleteExistingChannelCategories(channel.getId());
            return;
        }

        Map<Integer, Long> categoryCounts = recentVideos.stream()
                .map(Video::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (categoryCounts.isEmpty()) {
            deleteExistingChannelCategories(channel.getId());
            return;
        }

        List<Integer> topCategoryIds = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(CHANNEL_CATEGORY_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        Map<Integer, YoutubeCategory> categoriesByYoutubeId = youtubeCategoryRepository.findByYoutubeCategoryIdIn(topCategoryIds)
                .stream()
                .collect(Collectors.toMap(YoutubeCategory::getYoutubeCategoryId, Function.identity()));

        List<ChannelCategory> existingRelations = channelCategoryRepository.findAllByChannel_Id(channel.getId());
        Map<Long, ChannelCategory> existingByCategoryId = existingRelations.stream()
                .collect(Collectors.toMap(relation -> relation.getCategory().getId(), Function.identity(), (left, right) -> left));

        LinkedHashSet<Long> targetCategoryDbIds = topCategoryIds.stream()
                .map(categoriesByYoutubeId::get)
                .filter(Objects::nonNull)
                .map(YoutubeCategory::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ChannelCategory> toDelete = existingRelations.stream()
                .filter(relation -> !targetCategoryDbIds.contains(relation.getCategory().getId()))
                .toList();
        if (!toDelete.isEmpty()) {
            channelCategoryRepository.deleteAll(toDelete);
        }

        List<ChannelCategory> toCreate = targetCategoryDbIds.stream()
                .filter(categoryId -> !existingByCategoryId.containsKey(categoryId))
                .map(categoryId -> ChannelCategory.of(channel, findCategoryById(categoryId, categoriesByYoutubeId)))
                .filter(Objects::nonNull)
                .toList();
        if (!toCreate.isEmpty()) {
            channelCategoryRepository.saveAll(toCreate);
        }
    }

    private void deleteExistingChannelCategories(Long channelId) {
        List<ChannelCategory> existingRelations = channelCategoryRepository.findAllByChannel_Id(channelId);
        if (!existingRelations.isEmpty()) {
            channelCategoryRepository.deleteAll(existingRelations);
        }
    }

    private YoutubeCategory findCategoryById(Long categoryId, Map<Integer, YoutubeCategory> categoriesByYoutubeId) {
        return categoriesByYoutubeId.values().stream()
                .filter(category -> Objects.equals(category.getId(), categoryId))
                .findFirst()
                .orElse(null);
    }

    private String extractChannelThumbnailUrl(YoutubeDataChannelResponse.Thumbnails thumbnails) {
        if (thumbnails == null) {
            return null;
        }
        if (thumbnails.high() != null && StringUtils.hasText(thumbnails.high().url())) {
            return thumbnails.high().url();
        }
        if (thumbnails.medium() != null && StringUtils.hasText(thumbnails.medium().url())) {
            return thumbnails.medium().url();
        }
        if (thumbnails.defaultThumbnail() != null && StringUtils.hasText(thumbnails.defaultThumbnail().url())) {
            return thumbnails.defaultThumbnail().url();
        }
        return null;
    }

    private String extractVideoThumbnailUrl(YoutubeDataVideoResponse.Thumbnails thumbnails) {
        if (thumbnails == null || thumbnails.high() == null) {
            return null;
        }
        return thumbnails.high().url();
    }

    private String extractUploadsPlaylistId(YoutubeDataChannelResponse.ContentDetails contentDetails) {
        if (contentDetails == null || contentDetails.relatedPlaylists() == null) {
            return null;
        }
        return contentDetails.relatedPlaylists().uploads();
    }

    private boolean hasPaidProductPlacement(YoutubeDataVideoResponse.Item item) {
        return item.paidProductPlacementDetails() != null
                && item.paidProductPlacementDetails().hasPaidProductPlacement() != null
                && item.paidProductPlacementDetails().hasPaidProductPlacement() == true;
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (!StringUtils.hasText(publishedAt)) {
            return null;
        }
        return OffsetDateTime.parse(publishedAt).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private Integer parseDurationSeconds(YoutubeDataVideoResponse.ContentDetails contentDetails) {
        if (contentDetails == null || !StringUtils.hasText(contentDetails.duration())) {
            return null;
        }
        return (int) AnalyticsCalculator.parseIso8601Duration(contentDetails.duration());
    }

    private Long toLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    private Integer toInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private Double calculateViewsPerHour(Long videoViewCount, LocalDateTime publishedAt, LocalDateTime collectedAt) {
        if (publishedAt == null) {
            return null;
        }
        double elapsedSeconds = java.time.Duration.between(publishedAt, collectedAt).toSeconds();
        if (elapsedSeconds <= 0) {
            return null;
        }
        double elapsedHours = Math.max(elapsedSeconds / 3600.0, 1.0);
        return round(defaultLong(videoViewCount) / elapsedHours, 6);
    }

    private Double calculateOutlierScore(
            Long videoViewCount,
            ChannelStats channelStats,
            YoutubeDataChannelResponse.Statistics channelStatistics
    ) {
        long totalVideoCount = channelStats.getTotalVideoCount() != null
                ? channelStats.getTotalVideoCount()
                : defaultLong(channelStatistics == null ? null : toLong(channelStatistics.videoCount()));
        long totalViewCount = channelStats.getTotalViewCount() != null
                ? channelStats.getTotalViewCount()
                : defaultLong(channelStatistics == null ? null : toLong(channelStatistics.viewCount()));
        if (totalVideoCount <= 0) {
            return null;
        }
        double avgChannelViewCount = (double) totalViewCount / totalVideoCount;
        if (avgChannelViewCount <= 0) {
            return null;
        }
        return round(defaultLong(videoViewCount) / avgChannelViewCount, 6);
    }

    private Double calculateEngagementRate(Long likeCount, Long commentCount, Long viewCount) {
        return round(AnalyticsCalculator.engagementRate(likeCount, commentCount, viewCount), 6);
    }

    private Double calculateRisingScore(Double engagementRate, Double avgEngagementRate, Double outlierScore) {
        if (engagementRate == null || outlierScore == null || avgEngagementRate == null || avgEngagementRate <= 0) {
            return null;
        }
        return round((engagementRate / avgEngagementRate) * outlierScore, 6);
    }

    private double calculateAverageOutlierScoreRecentExcludingTop5Pct(
            List<Video> recentVideos,
            Map<Long, VideoStats> videoStatsMap,
            ChannelStats channelStats
    ) {
        List<Double> outlierScores = recentVideos.stream()
                .map(video -> videoStatsMap.get(video.getId()))
                .map(videoStats -> calculateOutlierScore(videoStats == null ? 0L : videoStats.getViewCount(), channelStats, null))
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .toList();

        if (outlierScores.isEmpty()) {
            return 0.0;
        }

        int excludedCount = (int) Math.floor(outlierScores.size() * 0.05d);
        int startIndex = Math.min(excludedCount, outlierScores.size());
        List<Double> retainedScores = outlierScores.subList(startIndex, outlierScores.size());
        if (retainedScores.isEmpty()) {
            return 0.0;
        }

        double average = retainedScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return round(average, 6);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
