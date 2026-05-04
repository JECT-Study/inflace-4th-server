package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.video.domain.Video;
import java.util.List;

public record ChannelDataSyncResult(
        Channel channel,
        List<Video> videos
) {
}
