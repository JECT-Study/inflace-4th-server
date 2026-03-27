package com.example.inflace.global.util;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 * Analytics api에서 사용하는 계산식 유틸
 */
public class AnalyticsCalculator {

    private static long nz(Long value) {
        return (value == null || value < 0) ? 0L : value;
    }

    /**
     * 영상 참여율
     *
     * @param likeCount    좋아요 수
     * @param commentCount 댓글 수
     * @param viewCount    조회수
     * @return 참여율 (%)
     */
    public static double engagementRate(Long likeCount, Long commentCount, Long viewCount) {
        if (nz(viewCount) == 0) {  // 0 나누기 방지
            return 0.0;
        }
        return ((nz(likeCount) + nz(commentCount)) / (double) nz(viewCount)) * 100;
    }

    /**
     * 신규 유입 비율
     *
     * @param unsubscribedViewCount 비구독자 조회수
     * @param viewCount             전체 조회수
     * @return 신규 유입 비율 (%)
     */
    public static double newViewerRate(Long unsubscribedViewCount, Long viewCount) {
        if (nz(viewCount) == 0) {
            return 0.0;
        }
        return (nz(unsubscribedViewCount) / (double) nz(viewCount)) * 100;
    }

    /**
     * OutLier (채널 평균 대비 조회수 배수)
     *
     * @param viewCount      해당 영상 조회수
     * @param totalViewCount 채널 전체 조회수
     * @param videoCount     채널 영상 수
     * @return 채널 평균 대비 배수
     */
    public static double outlier(Long viewCount, Long totalViewCount, Long videoCount) {
        if (nz(totalViewCount) == 0 || nz(videoCount) == 0) {
            return 0.0;
        }
        return nz(viewCount) / ((double) nz(totalViewCount) / nz(videoCount));
    }

    /**
     * VPH (시간당 조회수)
     *
     * @param viewCount   조회수
     * @param publishedAt 영상 업로드 시각
     * @return 시간당 조회수
     */
    public static double vph(Long viewCount, LocalDateTime publishedAt) {
        // 날짜 null 체크 및 미래 시간 방어
        if (publishedAt == null || publishedAt.isAfter(LocalDateTime.now())) {
            return 0.0;
        }
        long hours = HOURS.between(publishedAt, LocalDateTime.now());
        if (nz(hours) == 0) {
            return 0.0;
        }
        return nz(viewCount) / (double) nz(hours);
    }

    /**
     * 이탈률 (구간별)
     *
     * @param prevRetention = t-1 구간의 retentionRate
     * @param retention     = t구간의 retentionRate
     * @return
     */
    public static double churnRate(double prevRetention, double retention) {
        return prevRetention - retention;
    }

    /**
     * 퍼센트 변환 (잔존률 / 유튜브 평균 대비 유지율)
     *
     * @param value API 원본값 (0.0 ~ 1.0)
     * @return 퍼센트 값 (%)
     */
    public static double toPercent(double value) {
        return value * 100;
    }
}
