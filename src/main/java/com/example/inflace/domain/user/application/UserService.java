package com.example.inflace.domain.user.application;

import com.example.inflace.domain.auth.presentation.dto.UserDetailsResponse;
import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelCategory;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;
import com.example.inflace.domain.channel.repository.ChannelCategoryRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.entity.UserNeed;
import com.example.inflace.domain.user.domain.entity.UserType;
import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.domain.user.infra.UserCommandRepository;
import com.example.inflace.domain.user.infra.UserNeedRepository;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.domain.user.infra.UserTypeRepository;
import com.example.inflace.domain.user.presentation.OnboardingRequest;
import com.example.inflace.domain.user.presentation.ProfileImageUpdateRequest;
import com.example.inflace.domain.user.presentation.ProfileImageUploadUrlRequest;
import com.example.inflace.domain.user.presentation.ProfileImageUploadUrlResponse;
import com.example.inflace.domain.user.presentation.UserChannelMainResponse;
import com.example.inflace.domain.user.presentation.UserPreferenceUpdateRequest;
import com.example.inflace.domain.user.presentation.UserProfileResponse;
import com.example.inflace.domain.user.presentation.YoutubeLinkedResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.util.SecurityUtils;
import com.example.inflace.infra.aws.s3.S3ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String YOUTUBE_STUDIO_URL_PREFIX = "https://studio.youtube.com/channel/";

    private final UserReadRepository userReadRepository;
    private final UserCommandRepository userCommandRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserNeedRepository userNeedRepository;
    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository channelCategoryRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final VideoRepository videoRepository;
    private final S3ImageStorageService imageStorageService;
    private final AuthTokenRedisService authTokenRedisService;

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
        userCommandRepository.softDeleteUser(userId);
        authTokenRedisService.deleteRefreshToken(userId);
        SecurityUtils.clear();
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

        List<String> categories = channelCategoryRepository.findAllByChannel_Id(channel.getId()).stream()
                .map(ChannelCategory::getCategory)
                .map(YoutubeCategory::getTitle)
                .toList();

        return new UserChannelMainResponse(
                user.getProfileImage(),
                channel.getName(),
                YOUTUBE_STUDIO_URL_PREFIX + channel.getYoutubeChannelId(),
                channel.getChannelHandle(),
                categories,
                channel.getYoutubePublishedAt(),
                channelStats.getSubscriberCount(),
                (long) videos.size(),
                channel.getUpdatedAt()
        );
    }

    @ReadOnlyTransactional
    public UserProfileResponse getProfile() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        return new UserProfileResponse(
                new UserProfileResponse.AccountInfo(
                        user.getProfileImage(),
                        user.getName(),
                        user.getEmail(),
                        user.getCreatedAt()
                ),
                new UserProfileResponse.PreferenceInfo(
                        getUserRoles(userId),
                        getUserNeeds(userId)
                )
        );
    }

    @Transactional
    public UserProfileResponse updatePreferences(UserPreferenceUpdateRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        userTypeRepository.deleteAllByUserId(userId);
        userNeedRepository.deleteAllByUserId(userId);

        userTypeRepository.saveAll(request.roles().stream()
                .map(role -> UserType.of(role, user))
                .toList());
        userNeedRepository.saveAll(request.needs().stream()
                .map(need -> new UserNeed(need, user))
                .toList());

        return getProfile();
    }

    public ProfileImageUploadUrlResponse createProfileImageUploadUrl(ProfileImageUploadUrlRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        return imageStorageService.createProfileImageUploadUrl(userId, request.contentType(), request.fileSize());
    }

    @Transactional
    public UserProfileResponse updateProfileImage(ProfileImageUpdateRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        String profileImageUrl = imageStorageService.confirmProfileImageUpload(userId, request.objectKey());
        user.updateProfileImage(profileImageUrl);

        return getProfile();
    }

    @Transactional
    public void onboarding(OnboardingRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();

        userCommandRepository.insertUserTypes(userId, request.roles());
        userCommandRepository.insertNeeds(userId, request.needs());
    }

    private List<UserRole> getUserRoles(UUID userId) {
        return userTypeRepository.findAllByUser_Id(userId).stream()
                .map(UserType::getRole)
                .toList();
    }

    private List<Need> getUserNeeds(UUID userId) {
        return userNeedRepository.findAllByUser_Id(userId).stream()
                .map(UserNeed::getNeed)
                .toList();
    }
}
