package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.channel.dto.response.ChannelVideosResponse.ChannelVideoItem;
import java.util.List;

public record ChannelVideoSliceResult(
        List<ChannelVideoItem> videos,
        String nextCursor,
        boolean hasNext
) {
}
