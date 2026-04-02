package com.example.inflace.domain.channel.dto;

public record ChannelVideosRequest(
        String keyword,
        ChannelVideoSort sort,
        String cursor,
        Integer size
) {
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    public ChannelVideosRequest {
        sort = sort == null ? ChannelVideoSort.LATEST : sort;
        size = normalizeSize(size);
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
