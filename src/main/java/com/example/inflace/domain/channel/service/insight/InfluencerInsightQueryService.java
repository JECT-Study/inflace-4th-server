package com.example.inflace.domain.channel.service.insight;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelCategory;
import com.example.inflace.domain.channel.domain.ChannelStats;
import com.example.inflace.domain.channel.domain.YoutubeCategory;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.channel.repository.ChannelCategoryRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.service.VideoService;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InfluencerInsightQueryService {

    private static final int MIN_VIDEO_COUNT_FOR_INSIGHT = 50;
    private static final int MAX_RECENT_VIDEO_DESCRIPTION_COUNT = 10;

    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository channelStatsRepository;
    private final ChannelCategoryRepository channelCategoryRepository;
    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final InfluencerInsightCalculator influencerInsightCalculator;

    @ReadOnlyTransactional
    public InfluencerInsightQueryResult getInsightQueryResult(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        List<Video> videos = videoRepository.findByChannelIdOrderByPublishedAtDesc(channelId);
        if (videos.size() < MIN_VIDEO_COUNT_FOR_INSIGHT) {
            throw new ApiException(ErrorDefine.CHANNEL_INSIGHT_REQUIRES_MIN_VIDEO_COUNT);
        }
        List<String> categories = channelCategoryRepository.findAllByChannel_Id(channelId).stream()
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
                videoService.getVideoStatsMap(videos)
        );

        return new InfluencerInsightQueryResult(
                channel.getDescription(),
                getRecentVideoDescriptions(videos),
                insight
        );
    }

    private List<String> getRecentVideoDescriptions(List<Video> videos) {
        return videos.stream()
                .filter(video -> StringUtils.hasText(video.getDescription()))
                .limit(MAX_RECENT_VIDEO_DESCRIPTION_COUNT)
                .map(Video::getDescription)
                .toList();
    }
}
