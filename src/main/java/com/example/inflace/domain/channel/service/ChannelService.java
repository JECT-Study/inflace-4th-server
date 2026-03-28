package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.dto.YoutubeDataChannelResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.dto.ChannelTopVideosResponse;
import com.example.inflace.domain.video.dto.VideoType;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.client.YoutubeDataApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeDataApiClient youtubeDataApiClient;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;

    private YoutubeDataChannelResponse getYoutubeChannel(String channelId, String parts) {
        YoutubeDataChannelResponse response = youtubeDataApiClient.getYoutubeChannels(channelId, parts);
        return response;
    }


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
                parsedContentType == VideoType.SHORT_FORM,
                PageRequest.of(0, 5)
        );

        Map<Long, VideoStats> videoStatsMap = getVideoStatsMap(videos);
        List<ChannelTopVideosResponse.ChannelTopVideo> items = mapTopVideos(videos, videoStatsMap);
        return new ChannelTopVideosResponse(items);
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

    private List<ChannelTopVideosResponse.ChannelTopVideo> mapTopVideos(
            List<Video> videos,
            Map<Long, VideoStats> videoStatsMap
    ) {
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
                    (double) video.getRisingScore(),
                    videoStats != null && videoStats.getCtr() != null ? videoStats.getCtr() : 0.0,
                    videoStats != null && videoStats.getAverageViewPercentage() != null
                            ? videoStats.getAverageViewPercentage() : 0.0
            ));

            rank++;
        }

        return items;
    }
}
