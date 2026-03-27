package com.example.inflace.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class AnalyticsCalculatorTest {

    @Test
    @DisplayName("참여율 - 정상 계산")
    void engagementRate_정상계산() {
        // (100 + 50) / 1000 * 100 = 15.0
        double result = AnalyticsCalculator.engagementRate(100L, 50L, 1000L);
        assertThat(result).isEqualTo(15.0);
    }

    @Test
    @DisplayName("참여율 - 조회수 0이면 0 반환")
    void engagementRate_조회수가0이면_0반환() {
        double result = AnalyticsCalculator.engagementRate(100L, 50L, 0L);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("참여율 - 좋아요 댓글 모두 0이면 0 반환")
    void engagementRate_좋아요댓글이0이면_0반환() {
        double result = AnalyticsCalculator.engagementRate(0L, 0L, 1000L);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("신규 유입 비율 - 정상 계산")
    void newViewerRate_정상계산() {
        // 300 / 1000 * 100 = 30.0
        double result = AnalyticsCalculator.newViewerRate(300L, 1000L);
        assertThat(result).isEqualTo(30.0);
    }

    @Test
    @DisplayName("신규 유입 비율 - 조회수 0이면 0 반환")
    void newViewerRate_조회수가0이면_0반환() {
        double result = AnalyticsCalculator.newViewerRate(300L, 0L);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("신규 유입 비율 - 전부 신규이면 100 반환")
    void newViewerRate_전부신규이면_100반환() {
        double result = AnalyticsCalculator.newViewerRate(1000L, 1000L);
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    @DisplayName("아웃라이어 - 정상 계산")
    void outlier_정상계산() {
        // 1000 / (5000 / 10) = 1000 / 500 = 2.0
        double result = AnalyticsCalculator.outlier(1000L, 5000L, 10L);
        assertThat(result).isEqualTo(2.0);
    }

    @Test
    @DisplayName("아웃라이어 - 전체 조회수 0이면 0 반환")
    void outlier_전체조회수가0이면_0반환() {
        double result = AnalyticsCalculator.outlier(1000L, 0L, 10L);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("아웃라이어 - 영상 수 0이면 0 반환")
    void outlier_영상수가0이면_0반환() {
        double result = AnalyticsCalculator.outlier(1000L, 5000L, 0L);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("아웃라이어 - 평균과 동일하면 1.0 반환")
    void outlier_평균과동일하면_1반환() {
        // 500 / (5000 / 10) = 500 / 500 = 1.0
        double result = AnalyticsCalculator.outlier(500L, 5000L, 10L);
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VPH - 정상 계산")
    void vph_정상계산() {
        // 1000 views, 10시간 전 업로드 → 100.0
        LocalDateTime publishedAt = LocalDateTime.now().minusHours(10);
        double result = AnalyticsCalculator.vph(1000L, publishedAt);
        assertThat(result).isCloseTo(100.0, offset(1.0));
    }

    @Test
    @DisplayName("VPH - 방금 업로드(0시간)이면 0 반환")
    void vph_방금업로드이면_0반환() {
        LocalDateTime publishedAt = LocalDateTime.now();
        double result = AnalyticsCalculator.vph(1000L, publishedAt);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("이탈률 - 정상 계산")
    void churnRate_정상계산() {
        // 80% → 60% : 이탈률 20%
        double result = AnalyticsCalculator.churnRate(80.0, 60.0);
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    @DisplayName("이탈률 - 구간 유지율 동일하면 0 반환")
    void churnRate_유지율동일하면_0반환() {
        double result = AnalyticsCalculator.churnRate(70.0, 70.0);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("퍼센트 변환 - 0.5 → 50.0")
    void toPercent_정상변환() {
        double result = AnalyticsCalculator.toPercent(0.5);
        assertThat(result).isEqualTo(50.0);
    }

    @Test
    @DisplayName("퍼센트 변환 - 0.0 → 0.0")
    void toPercent_0이면_0반환() {
        double result = AnalyticsCalculator.toPercent(0.0);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("퍼센트 변환 - 1.0 → 100.0")
    void toPercent_1이면_100반환() {
        double result = AnalyticsCalculator.toPercent(1.0);
        assertThat(result).isEqualTo(100.0);
    }
}