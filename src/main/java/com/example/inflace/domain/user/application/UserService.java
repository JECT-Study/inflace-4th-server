package com.example.inflace.domain.user.application;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.infra.UserCommandRepository;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.domain.user.presentation.OnboardingRequest;
import com.example.inflace.domain.user.presentation.UserChannelMainResponse;
import com.example.inflace.domain.user.presentation.YoutubeChannelLinkResponse;
import com.example.inflace.domain.user.presentation.YoutubeLinkedResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.config.JwtProvider;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.service.YoutubeAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String YOUTUBE_STUDIO_URL_PREFIX = "https://studio.youtube.com/channel/";

    private final UserReadRepository userReadRepository;
    private final UserCommandRepository userCommandRepository;
    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final VideoRepository videoRepository;
    private final GoogleAccessTokenStore googleAccessTokenStore;
    private final YoutubeDataApiClient youtubeDataApiClient;
    private final YoutubeAnalyticsService youtubeAnalyticsService;
    private final JwtProvider jwtProvider;

    @Transactional
    public UserRegistrationResult registerIfNotExists(String sub, String name, String email, String profileImage, Plan plan) {
        return userCommandRepository.insertIfNotExists(sub, name, email, profileImage, plan);
    }

    @Transactional
    public void withdraw(long userId) {
        if (!userReadRepository.existsById(userId)) {
            throw new ApiException(ErrorDefine.USER_NOT_FOUND);
        }
        userCommandRepository.deleteUser(userId);
    }

    @Transactional(readOnly = true)
    public YoutubeLinkedResponse isYoutubeLinked(long userId) {
        return new YoutubeLinkedResponse(channelRepository.existsByUser_Id(userId));
    }

    @Transactional(readOnly = true)
    public UserChannelMainResponse getMainChannelInfo(long userId) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        Channel channel = channelRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channel.getId())
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));

        List<Video> videos = videoRepository.findByChannelId(channel.getId());

        List<String> category = channel.getCategory() != null
                ? Arrays.asList(channel.getCategory())
                : null;

        return new UserChannelMainResponse(
                user.getProfileImage(),
                channel.getName(),
                YOUTUBE_STUDIO_URL_PREFIX + channel.getYoutubeChannelId(),
                channel.getChannelHandle(),
                category,
                channel.getEnteredAt(),
                channelStats.getSubscriberCount(),
                (long) videos.size(),
                channel.getUpdatedAt()
        );
    }

    @Transactional
    public YoutubeChannelLinkResponse linkYoutubeChannel(long userId) {
        String accessToken = googleAccessTokenStore.getAccessToken(userId);
        if (accessToken == null) {
            throw new ApiException(ErrorDefine.YOUTUBE_TOKEN_NOT_FOUND);
        }
        YoutubeDataChannelResponse channelResponse = youtubeDataApiClient.getMyChannel(accessToken);

        if (channelResponse.items() == null || channelResponse.items().isEmpty()) {
            throw new ApiException(ErrorDefine.YOUTUBE_CHANNEL_NOT_FOUND);
        }

        YoutubeDataChannelResponse.Item item = channelResponse.items().get(0);
        String channelName = item.snippet().title();
        String profileImage = item.snippet().thumbnails().high().url();
        String youtubeChannelId = item.id();
        String channelHandle = item.snippet().customUrl();
        LocalDateTime enteredAt = item.snippet().publishedAt() != null
                ? LocalDateTime.ofInstant(Instant.parse(item.snippet().publishedAt()), ZoneOffset.UTC)
                : null;
        Long subscriberCount = item.statistics() != null && item.statistics().subscriberCount() != null
                ? Long.parseLong(item.statistics().subscriberCount())
                : null;
        Long totalViewCount = item.statistics() != null && item.statistics().viewCount() != null
                ? Long.parseLong(item.statistics().viewCount())
                : null;
        String[] categories = item.topicDetails() != null && item.topicDetails().topicCategories() != null
                ? item.topicDetails().topicCategories().stream()
                        .map(url -> url.substring(url.lastIndexOf('/') + 1).replace('_', ' '))
                        .toArray(String[]::new)
                : null;

        Optional<Channel> existingChannel = channelRepository.findByUser_Id(userId);
        Channel channel;
        if (existingChannel.isPresent()) {
            channel = existingChannel.get();
            channel.updateAll(channelName, profileImage, youtubeChannelId, channelHandle, categories, enteredAt);
        } else {
            User user = userReadRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
            channel = channelRepository.save(Channel.builder()
                    .user(user)
                    .name(channelName)
                    .profileImage(profileImage)
                    .youtubeChannelId(youtubeChannelId)
                    .channelHandle(channelHandle)
                    .category(categories)
                    .enteredAt(enteredAt)
                    .build());
        }

        Optional<ChannelStats> existingStats = channelStatsRepository.findByChannel_Id(channel.getId());
        if (existingStats.isPresent()) {
            existingStats.get().updateBasicStats(subscriberCount, totalViewCount, LocalDateTime.now());
        } else {
            channelStatsRepository.save(ChannelStats.builder()
                    .channel(channel)
                    .subscriberCount(subscriberCount)
                    .totalViewCount(totalViewCount)
                    .collectedAt(LocalDateTime.now())
                    .build());
        }

        // Analytics API로 추가 채널 통계 수집 (각 항목 독립 수집)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(28);

        Long subscriberViewCount = null;
        Double avgEngagementRate = null;
        Map<String, Double> audienceGender = null;
        Map<String, Double> audienceAge = null;
        Map<String, Double> audienceCountry = null;

        try {
            Map<String, Object> subscriberViewData = youtubeAnalyticsService.query(userId,
                    new YoutubeAnalyticsVideoRequest(startDate, endDate,
                            List.of("views"), null, null, "subscribedStatus==SUBSCRIBED"));
            if (!subscriberViewData.isEmpty()) {
                subscriberViewCount = ((Number) subscriberViewData.get("views")).longValue();
            }
        } catch (Exception e) {
            log.warn("구독자 조회수 수집 실패 (데이터 부족 또는 API 오류): userId={}, error={}", userId, e.getMessage());
        }

        try {
            Map<String, Object> engagementData = youtubeAnalyticsService.query(userId,
                    new YoutubeAnalyticsVideoRequest(startDate, endDate,
                            List.of("views", "likes", "comments", "shares"), null, null, null));
            if (!engagementData.isEmpty()) {
                double views = ((Number) engagementData.get("views")).doubleValue();
                double likes = ((Number) engagementData.getOrDefault("likes", 0)).doubleValue();
                double comments = ((Number) engagementData.getOrDefault("comments", 0)).doubleValue();
                double shares = ((Number) engagementData.getOrDefault("shares", 0)).doubleValue();
                if (views > 0) {
                    avgEngagementRate = (likes + comments + shares) / views * 100;
                }
            }
        } catch (Exception e) {
            log.warn("평균 참여율 수집 실패 (데이터 부족 또는 API 오류): userId={}, error={}", userId, e.getMessage());
        }

        try {
            List<Map<String, Object>> genderRows = youtubeAnalyticsService.queryAllRows(userId,
                    new YoutubeAnalyticsVideoRequest(startDate, endDate,
                            List.of("viewerPercentage"), null, "gender", null));
            if (!genderRows.isEmpty()) {
                Map<String, Double> genderMap = new HashMap<>();
                for (Map<String, Object> row : genderRows) {
                    genderMap.put((String) row.get("gender"),
                            ((Number) row.get("viewerPercentage")).doubleValue());
                }
                audienceGender = genderMap;
            }
        } catch (Exception e) {
            log.warn("성별 분포 수집 실패 (시청자 데이터 부족 또는 API 오류): userId={}, error={}", userId, e.getMessage());
        }

        try {
            List<Map<String, Object>> ageRows = youtubeAnalyticsService.queryAllRows(userId,
                    new YoutubeAnalyticsVideoRequest(startDate, endDate,
                            List.of("viewerPercentage"), null, "ageGroup", null));
            if (!ageRows.isEmpty()) {
                Map<String, Double> ageMap = new HashMap<>();
                for (Map<String, Object> row : ageRows) {
                    ageMap.put((String) row.get("ageGroup"),
                            ((Number) row.get("viewerPercentage")).doubleValue());
                }
                audienceAge = ageMap;
            }
        } catch (Exception e) {
            log.warn("연령 분포 수집 실패 (시청자 데이터 부족 또는 API 오류): userId={}, error={}", userId, e.getMessage());
        }

        try {
            List<Map<String, Object>> countryRows = youtubeAnalyticsService.queryAllRows(userId,
                    new YoutubeAnalyticsVideoRequest(startDate, endDate,
                            List.of("views"), null, "country", null));
            if (!countryRows.isEmpty()) {
                Map<String, Double> countryMap = new HashMap<>();
                for (Map<String, Object> row : countryRows) {
                    countryMap.put((String) row.get("country"),
                            ((Number) row.get("views")).doubleValue());
                }
                audienceCountry = countryMap;
            }
        } catch (Exception e) {
            log.warn("국가 분포 수집 실패 (시청자 데이터 부족 또는 API 오류): userId={}, error={}", userId, e.getMessage());
        }

        ChannelStats stats = channelStatsRepository.findByChannel_Id(channel.getId())
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_STATS_NOT_FOUND));
        stats.updateAnalyticsStats(subscriberViewCount, avgEngagementRate, audienceGender, audienceAge, audienceCountry);

        User updatedUser = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(
                userId,
                updatedUser.getProfileImage(),
                updatedUser.getPlan(),
                channelName,
                profileImage,
                updatedUser.isOnboardingCompleted()
        );

        return new YoutubeChannelLinkResponse(channelName, profileImage, newAccessToken);
    }

    @Transactional
    public void onboarding(long userId, OnboardingRequest request) {

        if (request.role() == null || request.need() == null || request.need().isEmpty()) {
            throw new ApiException(ErrorDefine.ONBOARDING_INVALID_REQUEST);
        }

        userCommandRepository.insertUserType(userId, request.role().name());
        userCommandRepository.bulkInsertNeeds(userId, request.need());
        userCommandRepository.updateOnboardingCompleted(userId);
    }
}
