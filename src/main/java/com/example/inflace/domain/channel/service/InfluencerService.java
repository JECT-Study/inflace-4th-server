package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.*;
import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.request.InfluencerSortCriteria;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerBookmarksResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.channel.repository.ChannelBookmarkRepository;
import com.example.inflace.domain.channel.repository.ChannelCategoryRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.channel.repository.querydsl.CustomInfluencerQueryRepository;
import com.example.inflace.domain.channel.repository.querydsl.InfluencerCursorCodec;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.enums.SortOrder;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.infra.openai.OpenAiModel;
import com.example.inflace.infra.openai.OpenAiSendRequest;
import com.example.inflace.infra.openai.prompt.InfluencerInsightPrompt;
import com.example.inflace.infra.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InfluencerService {

    private final CustomInfluencerQueryRepository influencerQueryRepository;
    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final ChannelCategoryRepository channelCategoryRepository;
    private final ChannelBookmarkRepository channelBookmarkRepository;
    private final UserReadRepository userReadRepository;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final InfluencerCursorCodec influencerCursorCodec;
    private final InfluencerInsightCalculator influencerInsightCalculator;
    private final OpenAiService openAiService;

    private static final int MIN_VIDEO_COUNT_FOR_INSIGHT = 50;

    @ReadOnlyTransactional
    public CursorSliceResponse<GetInfluencerSearchResponse> getInfluencersWithSearchCondition(
            InfluencerSearchCondition searchCondition,
            UUID userId
    ) {
        InfluencerSortCriteria sortCriteria = searchCondition.sortCriteriaEnum();
        SortOrder sortOrder = searchCondition.sortOrder();

        InfluencerCursorCodec.DecodedInfluencerCursor cursor = influencerCursorCodec.decodeOrNull(
                searchCondition.cursor(),
                sortCriteria,
                sortOrder
        );

        Slice<GetInfluencerSearchResponse> slice = influencerQueryRepository.getInfluencersWithSearchCondition(
                searchCondition,
                userId,
                cursor
        );

        return CursorSliceResponse.from(
                slice,
                sortCriteria.value(),
                sortOrder.name(),
                buildNextCursor(slice, sortCriteria, sortOrder)
        );
    }

    @Transactional
    public void createChannelBookmark(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        User user = userReadRepository.getReferenceById(userId);

        ChannelBookmark channelBookmark = ChannelBookmark.of(channel, user);
        channelBookmarkRepository.save(channelBookmark);
    }

    @Transactional
    public void deleteChannelBookmark(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        channelBookmarkRepository.deleteByChannelAndUser(channel, userReadRepository.getReferenceById(userId));
    }

    @ReadOnlyTransactional
    public GetInfluencerBookmarksResponse getInfluencerBookmarks(UUID userId) {
        return new GetInfluencerBookmarksResponse(
                channelBookmarkRepository.findByUserId(userId).stream()
                        .map(ChannelBookmark::getChannel)
                        .map(Channel::getId)
                        .toList()
        );
    }

    @ReadOnlyTransactional
    public GetInfluencerInsightResponse getInfluencerInsight(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        List<Video> videos = videoRepository.findByChannelId(channelId);
        if (videos.size() < MIN_VIDEO_COUNT_FOR_INSIGHT) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }

        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        List<ChannelCategory> channelCategories = channelCategoryRepository.findAllByChannel_Id(channelId);
        List<String> categories = channelCategories.stream()
                .map(ChannelCategory::getCategory)
                .filter(Objects::nonNull)
                .map(YoutubeCategory::getTitle)
                .distinct()
                .toList();

        ChannelStats channelStats = channelStatsRepository.findByChannel_Id(channelId).orElse(null);
        GetInfluencerInsightResponse insight = influencerInsightCalculator.calculate(
                channel,
                channelStats,
                categories,
                videos,
                videoStatsMap
        );

        List<String> recentVideoDescriptions = getRecentVideoDescriptions(videos);

        String summary = null;

        try {
            summary = openAiService.sendChatMessage(new OpenAiSendRequest(
                    InfluencerInsightPrompt.systemMessage(),
                    InfluencerInsightPrompt.humanMessage(channel.getDescription(), recentVideoDescriptions, insight),
                    null,
                    OpenAiModel.GPT_4O_MINI
            ));
        } catch (RuntimeException ignored) {
        }

        return insight.withAiSummary(new GetInfluencerInsightResponse.AiSummary(
                summary
        ));
    }

    private String buildNextCursor(
            Slice<GetInfluencerSearchResponse> slice,
            InfluencerSortCriteria sortCriteria,
            SortOrder sortOrder
    ) {
        if (!slice.hasNext() || slice.isEmpty()) {
            return null;
        }

        GetInfluencerSearchResponse last = slice.getContent().get(slice.getNumberOfElements() - 1);

        return switch (sortCriteria) {
            case SUBSCRIBER -> influencerCursorCodec.encode(
                    sortCriteria,
                    sortOrder,
                    last.subscriberCount(),
                    last.channelId()
            );
            case ENGAGEMENT_RATE -> influencerCursorCodec.encode(
                    sortCriteria,
                    sortOrder,
                    last.averageEngagementRate(),
                    last.channelId()
            );
        };
    }

    private Map<Long, VideoStats> getVideoStatsMap(List<Video> videos) {
        if (videos.isEmpty()) {
            return Map.of();
        }

        List<Long> videoIds = videos.stream()
                .map(Video::getId)
                .toList();

        List<VideoStats> videoStatsList = videoStatsRepository.findAllByVideoIdIn(videoIds);
        Map<Long, VideoStats> result = new HashMap<>();
        for (VideoStats videoStats : videoStatsList) {
            result.put(videoStats.getVideo().getId(), videoStats);
        }
        return result;
    }

    private List<String> getRecentVideoDescriptions(List<Video> videos) {
        return videos.stream()
                .filter(video -> StringUtils.hasText(video.getDescription()))
                .sorted((video1, video2) -> {
                    if (video1.getPublishedAt() == null && video2.getPublishedAt() == null) {
                        return 0;
                    }
                    if (video1.getPublishedAt() == null) {
                        return 1;
                    }
                    if (video2.getPublishedAt() == null) {
                        return -1;
                    }
                    return video2.getPublishedAt().compareTo(video1.getPublishedAt());
                })
                .limit(10)
                .map(Video::getDescription)
                .toList();
    }
}
