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
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.config.JwtProvider;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

        Optional<Channel> existingChannel = channelRepository.findByUser_Id(userId);
        if (existingChannel.isPresent()) {
            existingChannel.get().updateProfile(channelName, profileImage);
        } else {
            User user = userReadRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
            channelRepository.save(Channel.builder()
                    .user(user)
                    .name(channelName)
                    .profileImage(profileImage)
                    .build());
        }

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
