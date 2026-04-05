package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.channel.dto.ChannelVideoSliceResult;
import com.example.inflace.domain.channel.dto.ChannelVideosRequest;

public interface VideoQueryRepository {
    ChannelVideoSliceResult findChannelVideos(Long channelId, ChannelVideosRequest request);
}
