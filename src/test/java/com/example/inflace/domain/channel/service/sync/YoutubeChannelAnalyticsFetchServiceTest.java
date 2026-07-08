package com.example.inflace.domain.channel.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.inflace.domain.channel.dto.sync.AnalyticsSyncContext;
import com.example.inflace.domain.channel.dto.sync.YoutubeAnalyticsSyncData;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YoutubeChannelAnalyticsFetchServiceTest {

    private static final String GOOGLE_ID = "google-1";

    private YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;
    private ExecutorService externalApiExecutor;
    private YoutubeChannelAnalyticsFetchService service;

    @BeforeEach
    void setUp() {
        youtubeAnalyticsApiClient = mock(YoutubeAnalyticsApiClient.class);
        externalApiExecutor = Executors.newVirtualThreadPerTaskExecutor();
        service = new YoutubeChannelAnalyticsFetchService(youtubeAnalyticsApiClient, externalApiExecutor);
    }

    @AfterEach
    void tearDown() {
        externalApiExecutor.close();
    }

    @Test
    void fetchAnalytics_runsVideoSummaryRequestsConcurrently() {
        AtomicInteger activeVideoSummaryCalls = new AtomicInteger();
        AtomicInteger maxActiveVideoSummaryCalls = new AtomicInteger();
        stubAnalyticsApi(activeVideoSummaryCalls, maxActiveVideoSummaryCalls, false);

        YoutubeAnalyticsSyncData data = service.fetchAnalytics(GOOGLE_ID, contextWithVideos(4));

        assertThat(maxActiveVideoSummaryCalls.get())
                .as("expected concurrent video analytics fetches but got sequential execution")
                .isGreaterThan(1);
        assertThat(data.videoAnalytics())
                .extracting(YoutubeAnalyticsSyncData.VideoAnalyticsData::videoId)
                .containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void fetchAnalytics_keepsPartialResultWhenOneVideoSummaryFails() {
        stubAnalyticsApi(new AtomicInteger(), new AtomicInteger(), true);

        YoutubeAnalyticsSyncData data = service.fetchAnalytics(GOOGLE_ID, contextWithVideos(3));

        assertThat(data.videoAnalytics()).hasSize(3);
        assertThat(data.videoAnalytics())
                .filteredOn(result -> result.videoId().equals(2L))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.updateSummary()).isTrue();
                    assertThat(result.shareCount()).isNull();
                    assertThat(result.unsubscribedViewCount()).isNull();
                });
    }

    private void stubAnalyticsApi(
            AtomicInteger activeVideoSummaryCalls,
            AtomicInteger maxActiveVideoSummaryCalls,
            boolean failSecondVideoSummary
    ) {
        when(youtubeAnalyticsApiClient.getYoutubeAnalytics(eq(GOOGLE_ID), any(YoutubeAnalyticsVideoRequest.class)))
                .thenAnswer(invocation -> {
                    YoutubeAnalyticsVideoRequest request = invocation.getArgument(1);
                    if ("video".equals(request.dimensions())) {
                        if (failSecondVideoSummary && "youtube-video-2".equals(request.youtubeVideoId())) {
                            throw new ApiException(ErrorDefine.YOUTUBE_API_ERROR);
                        }
                        return videoSummaryResponse(activeVideoSummaryCalls, maxActiveVideoSummaryCalls);
                    }
                    if ("subscribedStatus".equals(request.dimensions()) && request.youtubeVideoId() != null) {
                        return unsubscribedResponse();
                    }
                    if ("elapsedVideoTimeRatio".equals(request.dimensions())) {
                        return retentionResponse();
                    }
                    return emptyResponse();
                });
    }

    private YoutubeAnalyticsVideoResponse videoSummaryResponse(
            AtomicInteger activeVideoSummaryCalls,
            AtomicInteger maxActiveVideoSummaryCalls
    ) throws InterruptedException {
        int active = activeVideoSummaryCalls.incrementAndGet();
        maxActiveVideoSummaryCalls.accumulateAndGet(active, Math::max);
        try {
            Thread.sleep(80);
            return new YoutubeAnalyticsVideoResponse(
                    "youtubeAnalytics#resultTable",
                    List.of(
                            header("shares"),
                            header("averageViewDuration"),
                            header("averageViewPercentage"),
                            header("subscribersGained"),
                            header("annotationClickThroughRate")
                    ),
                    List.of(List.of(10, 20.0, 30.0, 40, 50.0))
            );
        } finally {
            activeVideoSummaryCalls.decrementAndGet();
        }
    }

    private YoutubeAnalyticsVideoResponse unsubscribedResponse() {
        return new YoutubeAnalyticsVideoResponse(
                "youtubeAnalytics#resultTable",
                List.of(header("subscribedStatus"), header("views")),
                List.of(List.of("UNSUBSCRIBED", 5))
        );
    }

    private YoutubeAnalyticsVideoResponse retentionResponse() {
        return new YoutubeAnalyticsVideoResponse(
                "youtubeAnalytics#resultTable",
                List.of(header("elapsedVideoTimeRatio"), header("audienceWatchRatio"), header("relativeRetentionPerformance")),
                List.of(List.of(0.1, 0.9, 1.1))
        );
    }

    private YoutubeAnalyticsVideoResponse emptyResponse() {
        return new YoutubeAnalyticsVideoResponse("youtubeAnalytics#resultTable", List.of(), List.of());
    }

    private YoutubeAnalyticsVideoResponse.ColumnHeader header(String name) {
        return new YoutubeAnalyticsVideoResponse.ColumnHeader(name, "INTEGER", "METRIC");
    }

    private AnalyticsSyncContext contextWithVideos(int videoCount) {
        List<AnalyticsSyncContext.VideoContext> videos = java.util.stream.IntStream.rangeClosed(1, videoCount)
                .mapToObj(index -> new AnalyticsSyncContext.VideoContext(
                        (long) index,
                        "youtube-video-" + index,
                        LocalDateTime.now().minusDays(10),
                        false,
                        100L
                ))
                .toList();

        return new AnalyticsSyncContext(
                1L,
                "youtube-channel-1",
                LocalDateTime.now().minusYears(1),
                1000L,
                null,
                videos
        );
    }
}
