package com.example.inflace.domain.channel.service.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.channel.repository.ChannelCategoryRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.ChannelStatsRepository;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.service.VideoService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InfluencerInsightQueryServiceTest {

    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @Mock
    ChannelRepository channelRepository;

    @Mock
    ChannelStatsRepository channelStatsRepository;

    @Mock
    ChannelCategoryRepository channelCategoryRepository;

    @Mock
    VideoRepository videoRepository;

    @Mock
    VideoService videoService;

    @Mock
    InfluencerInsightCalculator influencerInsightCalculator;

    @InjectMocks
    InfluencerInsightQueryService queryService;

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void getInsightQueryResultUsesBoundedQueriesAndUtcRecentWindow() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Seoul")));
        Channel channel = Channel.builder()
                .name("test-channel")
                .build();
        ReflectionTestUtils.setField(channel, "id", 1L);
        given(channelRepository.findById(1L)).willReturn(Optional.of(channel));
        given(videoRepository.countByChannelId(1L)).willReturn(10L);
        List<Video> latestVideos = List.of(
                Video.builder()
                        .description("recent-description")
                        .build()
        );
        given(videoRepository.findByChannelIdOrderByPublishedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(latestVideos);
        given(videoRepository.findFormatStatsRowsByChannelIdAndPublishedAtGreaterThanEqual(eq(1L), any()))
                .willReturn(List.of());
        given(channelCategoryRepository.findAllByChannel_Id(1L)).willReturn(List.of());
        given(channelStatsRepository.findByChannel_Id(1L)).willReturn(Optional.empty());
        given(videoService.getVideoStatsMap(latestVideos)).willReturn(java.util.Map.of());
        given(influencerInsightCalculator.calculate(any(), any(), any(), any(), any(), any()))
                .willReturn(insightResponse());

        LocalDateTime beforeUtcCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        InfluencerInsightQueryResult result = queryService.getInsightQueryResult(1L);

        ArgumentCaptor<Pageable> latestPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findByChannelIdOrderByPublishedAtDesc(eq(1L), latestPageableCaptor.capture());
        assertThat(latestPageableCaptor.getValue().getPageSize())
                .as("expected latest video query to load at most 50 videos")
                .isEqualTo(50);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoRepository).findFormatStatsRowsByChannelIdAndPublishedAtGreaterThanEqual(
                eq(1L),
                cutoffCaptor.capture()
        );
        assertThat(cutoffCaptor.getValue())
                .as("expected recent format cutoff to be based on UTC, not default JVM timezone")
                .isBetween(beforeUtcCutoff, LocalDateTime.now(ZoneOffset.UTC).minusDays(30));

        assertThat(result.recentVideoDescriptions())
                .as("expected recent descriptions to be reused from latest 50 videos")
                .containsExactly("recent-description");
    }

    private GetInfluencerInsightResponse insightResponse() {
        return new GetInfluencerInsightResponse(
                1L,
                "test-channel",
                null,
                null,
                null,
                null,
                0L,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                new GetInfluencerInsightResponse.FormatAnalysis(
                        new GetInfluencerInsightResponse.FormatMetric(0, 0.0, 0.0),
                        new GetInfluencerInsightResponse.FormatMetric(0, 0.0, 0.0)
                )
        );
    }
}
