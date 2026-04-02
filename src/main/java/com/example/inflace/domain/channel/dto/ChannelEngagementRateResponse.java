package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.dto.VideoType;
import java.util.List;

public record ChannelEngagementRateResponse(
        Summary summary,
        List<EngageVideo> videos
) {
    public record Summary(
            Double longFormAverageEngagementRate,
            Double shortFormAverageEngagementRate
    ) {
    }

    public record EngageVideo(
            int rank,
            Long videoId,
            String title,
            String thumbnailUrl,
            String contentType,
            Double engagementRate
    ) {
        public static EngageVideo from(int rank, Video video, double engagementRate) {
            return new EngageVideo(
                    rank,
                    video.getId(),
                    video.getTitle(),
                    video.getThumbnailUrl(),
                    video.isShort() ? VideoType.SHORT_FORM.name() : VideoType.LONG_FORM.name(),
                    engagementRate
            );
        }
    }
}
