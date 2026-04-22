package com.example.inflace.domain.user.application;

import com.example.inflace.domain.auth.presentation.dto.UserDetailsResponse;
import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.domain.user.infra.UserCommandRepository;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.domain.user.presentation.OnboardingRequest;
import com.example.inflace.domain.user.presentation.UserChannelMainResponse;
import com.example.inflace.domain.user.presentation.YoutubeLinkedResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String YOUTUBE_STUDIO_URL_PREFIX = "https://studio.youtube.com/channel/";

    private final UserReadRepository userReadRepository;
    private final UserCommandRepository userCommandRepository;
    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public UserRegistrationResult registerIfNotExists(String sub, String name, String email, String profileImage, Plan plan) {
        return userCommandRepository.insertIfNotExists(sub, name, email, profileImage, plan);
    }

    @ReadOnlyTransactional
    public UserDetailsResponse getUserDetails(UUID userId) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
        List<UserRole> userRoles = userReadRepository.findUserRolesByUserId(userId);

        return UserDetailsResponse.of(user, userRoles);
    }

    @Transactional
    public void withdraw() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        userCommandRepository.deleteUser(userId);
    }

    @ReadOnlyTransactional
    public YoutubeLinkedResponse isYoutubeLinked() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        return new YoutubeLinkedResponse(channelRepository.existsByUser_Id(userId));
    }

    @ReadOnlyTransactional
    public UserChannelMainResponse getMainChannelInfo() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.getReferenceById(userId);

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
    public void onboarding(OnboardingRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();

        userCommandRepository.insertUserTypes(userId, request.roles());
        userCommandRepository.insertNeeds(userId, request.needs());
    }
}
