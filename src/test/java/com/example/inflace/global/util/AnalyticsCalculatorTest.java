package com.example.inflace.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

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
    @DisplayName("참여율 - 조회수 null이면 0 반환")
    void engagementRate_조회수null이면_0반환() {
        double result = AnalyticsCalculator.engagementRate(100L, 50L, null);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("참여율 - 좋아요 null이면 댓글만 계산")
    void engagementRate_좋아요null이면_댓글만계산() {
        // (0 + 50) / 1000 * 100 = 5.0
        double result = AnalyticsCalculator.engagementRate(null, 50L, 1000L);
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("참여율 - 음수 조회수는 0으로 처리")
    void engagementRate_음수조회수이면_0반환() {
        double result = AnalyticsCalculator.engagementRate(100L, 50L, -1L);
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
    @DisplayName("신규 유입 비율 - 조회수 null이면 0 반환")
    void newViewerRate_조회수null이면_0반환() {
        double result = AnalyticsCalculator.newViewerRate(300L, null);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("신규 유입 비율 - 비구독자 수 null이면 0 반환")
    void newViewerRate_비구독자null이면_0반환() {
        double result = AnalyticsCalculator.newViewerRate(null, 1000L);
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
    @DisplayName("아웃라이어 - 전체 조회수 null이면 0 반환")
    void outlier_전체조회수null이면_0반환() {
        double result = AnalyticsCalculator.outlier(1000L, null, 10L);
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
    @DisplayName("VPH - publishedAt null이면 0 반환")
    void vph_publishedAt_null이면_0반환() {
        double result = AnalyticsCalculator.vph(1000L, null);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("VPH - 미래 시간이면 0 반환")
    void vph_미래시간이면_0반환() {
        LocalDateTime future = LocalDateTime.now().plusHours(5);
        double result = AnalyticsCalculator.vph(1000L, future);
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

    @Test
    @DisplayName("구간 평균 이탈률 - 균등 감소 구간 정상 계산")
    void avgChurnRate_균등감소_정상계산() {
        // [10.0, 8.0, 6.0, 4.0] → diffs = [2.0, 2.0, 2.0] → avg = 2.0
        List<Double> rates = List.of(10.0, 8.0, 6.0, 4.0);

        double result = AnalyticsCalculator.avgChurnRate(rates);

        assertThat(result).isEqualTo(2.0);
    }

    @Test
    @DisplayName("구간 평균 이탈률 - 원소 2개이면 단일 차이값 반환")
    void avgChurnRate_원소2개이면_단일차이값반환() {
        // [10.0, 4.0] → diff = 6.0 → avg = 6.0
        List<Double> rates = List.of(10.0, 4.0);

        double result = AnalyticsCalculator.avgChurnRate(rates);

        assertThat(result).isEqualTo(6.0);
    }

    @Test
    @DisplayName("구간 평균 이탈률 - 반복 시청으로 유지율 증가 시 음수 반환")
    void avgChurnRate_유지율증가시_음수반환() {
        // [5.0, 8.0, 6.0] → diffs = [-3.0, 2.0] → avg = -0.5
        List<Double> rates = List.of(5.0, 8.0, 6.0);

        double result = AnalyticsCalculator.avgChurnRate(rates);

        assertThat(result).isEqualTo(-0.5);
    }

    @Test
    @DisplayName("구간 평균 이탈률 - null 리스트이면 0 반환")
    void avgChurnRate_null이면_0반환() {
        double result = AnalyticsCalculator.avgChurnRate(null);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("구간 평균 이탈률 - 원소 1개이면 0 반환")
    void avgChurnRate_원소1개이면_0반환() {
        List<Double> rates = List.of(10.0);

        double result = AnalyticsCalculator.avgChurnRate(rates);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("시간 포맷 변환 - 분:초 정상 변환")
    void formatTime_분초_정상변환() {
        // 0.5 * 600 = 300s → "5:00"
        String result = AnalyticsCalculator.formatTime(0.5, 600.0);

        assertThat(result).isEqualTo("5:00");
    }

    @Test
    @DisplayName("시간 포맷 변환 - 초가 한 자리면 0 패딩")
    void formatTime_초한자리이면_0패딩() {
        // 0.01 * 600 = 6s → "0:06"
        String result = AnalyticsCalculator.formatTime(0.01, 600.0);

        assertThat(result).isEqualTo("0:06");
    }

    @Test
    @DisplayName("시간 포맷 변환 - 정확히 분 단위이면 초 00")
    void formatTime_정확히분단위이면_초00() {
        // 0.1 * 600 = 60s → "1:00"
        String result = AnalyticsCalculator.formatTime(0.1, 600.0);

        assertThat(result).isEqualTo("1:00");
    }

    @Test
    @DisplayName("시간 포맷 변환 - 반올림 적용")
    void formatTime_반올림적용() {
        // 0.25 * 363 = 90.75 → round → 91s → "1:31"
        String result = AnalyticsCalculator.formatTime(0.25, 363.0);

        assertThat(result).isEqualTo("1:31");
    }
}