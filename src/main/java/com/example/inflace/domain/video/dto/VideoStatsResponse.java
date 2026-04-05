package com.example.inflace.domain.video.dto;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.global.util.AnalyticsCalculator;

import java.time.LocalDateTime;

import static com.example.inflace.global.util.AnalyticsParser.safeDoubleValue;

public record VideoStatsResponse(
        LocalDateTime collectedAt,
        StatValue viewCount,
        StatValue likeCount,
        StatValue commentCount,
        StatValue shareCount,
        StatValue subscribersGained,
        StatValue ctr,
        StatValue engagementRate,
        StatValue newViewerRate,
        StatValue outlier,
        StatValue vph
) {
    public record StatValue(
            Double value,  // Jackson 직렬화할 때 타입 맞추기 위해 Double로 통일
            Double changeRate
    ) {
    }

    public static VideoStatsResponse from(VideoStats stats, Long totalViewCount, Long videoCount) {
        Video video = stats.getVideo();

        // TODO : 변화율은 이전 값을 스냅샷으로 저장할 히스토리 엔티티가 따로 필요, 배치 구현 시 추가 기능으로 붙이는 방안 생각중입니다..
        return new VideoStatsResponse(
                stats.getCollectedAt(),
                new StatValue(safeDoubleValue(stats.getViewCount()), null),
                new StatValue(safeDoubleValue(stats.getLikeCount()), null),
                new StatValue(safeDoubleValue(stats.getCommentCount()), null),
                new StatValue(safeDoubleValue(stats.getShareCount()), null),
                new StatValue(safeDoubleValue(stats.getSubscribersGained()), null),
                new StatValue(stats.getCtr(), null),
                new StatValue(AnalyticsCalculator.engagementRate(stats.getLikeCount(), stats.getCommentCount(), stats.getViewCount()), null),
                new StatValue(AnalyticsCalculator.newViewerRate(stats.getUnsubscribedViewCount(), stats.getViewCount()), null),
                new StatValue(AnalyticsCalculator.outlier(stats.getViewCount(), totalViewCount, videoCount), null),
                new StatValue(AnalyticsCalculator.vph(stats.getViewCount(), video.getPublishedAt()), null)
        );
    }
}
