package com.example.inflace.domain.video.service;

import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.repository.AudienceRetentionRepository;
import com.example.inflace.domain.video.repository.VideoRepository;
import com.example.inflace.domain.video.repository.VideoStatsRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VideoServiceTest {

    @Mock
    VideoRepository videoRepository;

    @Mock
    VideoStatsRepository videoStatsRepository;

    @Mock
    AudienceRetentionRepository audienceRetentionRepository;

    @InjectMocks
    VideoService videoService;

    private Video video;
    private AudienceRetention retention;

    @BeforeEach
    void setUp() {
        video = Video.builder()
                .title("테스트 영상")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .build();
        ReflectionTestUtils.setField(video, "id", 1L);

        retention = AudienceRetention.builder()
                .video(video)
                .timeRatio(0.01)
                .retentionRate(98.0)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 시청_지속률_정상_조회() {
        // given
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoId(1L)).willReturn(List.of(retention));

        // when
        AudienceRetentionResponse response = videoService.getRetention(1L);

        // then
        assertThat(response.retentionData()).hasSize(1);
        assertThat(response.retentionData().get(0).timeRatio()).isEqualTo(0.01);
        assertThat(response.retentionData().get(0).watchRatio()).isEqualTo(98.0);
    }

    @Test
    void 시청_지속률_조회시_영상이_없으면_VIDEO_NOT_FOUND_예외() {
        // given
        given(videoRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> videoService.getRetention(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.VIDEO_NOT_FOUND);
    }

    @Test
    void 시청_지속률_조회시_데이터가_없으면_RETENTION_NOT_FOUND_예외() {
        // given
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoId(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> videoService.getRetention(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.RETENTION_NOT_FOUND);
    }
}
