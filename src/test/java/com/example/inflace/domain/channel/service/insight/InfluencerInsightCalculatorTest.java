package com.example.inflace.domain.channel.service.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import com.example.inflace.domain.video.repository.projection.VideoFormatStatsProjection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InfluencerInsightCalculatorTest {

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
}
