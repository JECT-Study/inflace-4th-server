package com.example.inflace.domain.channel.service.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.repository.projection.VideoFormatStatsProjection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InfluencerInsightCalculatorTest {

    @Test
    void viralRatesUseActualMetricCountWhenLatestVideosAreLessThanFifty() {
        InfluencerInsightCalculator calculator = new InfluencerInsightCalculator(new InfluencerInsightScoreCalculator());
        Channel channel = Channel.builder()
                .name("test-channel")
                .build();
        List<Long> viewCounts = List.of(1000L, 500L, 400L, 10L, 10L, 10L, 10L, 10L, 10L, 10L);
        List<Video> videos = new ArrayList<>();
        Map<Long, VideoStats> statsMap = new HashMap<>();

        for (int i = 0; i < viewCounts.size(); i++) {
            Video video = video((long) i + 1, LocalDateTime.now().minusDays(i));
            videos.add(video);
            statsMap.put(video.getId(), VideoStats.builder()
                    .video(video)
                    .viewCount(viewCounts.get(i))
                    .likeCount(0L)
                    .commentCount(0L)
                    .build());
        }

        GetInfluencerInsightResponse response = calculator.calculate(
                channel,
                null,
                List.of(),
                videos,
                statsMap,
                List.of()
        );

        assertThat(response.content().viral2xRate())
                .as("expected viral2xRate to use actual 10-video denominator, not fixed 50 denominator")
                .isEqualTo(30.0);
        assertThat(response.content().viral5xRate())
                .as("expected viral5xRate to use actual 10-video denominator, not fixed 50 denominator")
                .isEqualTo(10.0);
    }

    @Test
    void formatAnalysisUsesRecentThirtyDayProjectionRows() {
        InfluencerInsightCalculator calculator = new InfluencerInsightCalculator(new InfluencerInsightScoreCalculator());
        Video video = video();
        VideoStats stats = VideoStats.builder()
                .video(video)
                .viewCount(100L)
                .likeCount(10L)
                .commentCount(5L)
                .build();

        GetInfluencerInsightResponse response = calculator.calculate(
                Channel.builder().name("test-channel").build(),
                null,
                List.of(),
                List.of(video),
                Map.of(video.getId(), stats),
                List.of(
                        new VideoFormatStatsProjection(false, 100L, 10L, 5L),
                        new VideoFormatStatsProjection(true, 50L, 5L, 5L)
                )
        );

        assertThat(response.formatAnalysis().longForm().count())
                .as("expected long-form count from recent 30-day projection rows")
                .isOne();
        assertThat(response.formatAnalysis().shortForm().count())
                .as("expected short-form count from recent 30-day projection rows")
                .isOne();
    }

    private Video video() {
        Video video = Video.builder()
                .title("test-video")
                .isShort(false)
                .build();
        ReflectionTestUtils.setField(video, "id", 1L);
        return video;
    }

    private Video video(Long id, LocalDateTime publishedAt) {
        Video video = Video.builder()
                .title("test-video-" + id)
                .publishedAt(publishedAt)
                .isShort(false)
                .build();
        ReflectionTestUtils.setField(video, "id", id);
        return video;
    }
}
