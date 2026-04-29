package com.example.inflace.domain.channel.dto.response;

import java.util.List;

public record GetInfluencerBookmarksResponse(
        List<Long> channelIds
) {
}
