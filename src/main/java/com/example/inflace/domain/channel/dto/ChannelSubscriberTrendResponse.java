package com.example.inflace.domain.channel.dto;

import java.util.List;

public record ChannelSubscriberTrendResponse(
        String range,
        List<Point> points
) {
    public record Point(
            String date,
            Long subscriberCount
    ) {
    }
}
