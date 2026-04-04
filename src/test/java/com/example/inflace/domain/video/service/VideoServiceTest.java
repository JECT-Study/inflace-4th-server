package com.example.inflace.domain.video.service;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.video.domain.AudienceRetention;
import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.dto.AudienceRetentionResponse;
import com.example.inflace.domain.video.dto.DropPointsResponse;
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
import java.util.ArrayList;
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
    private List<AudienceRetention> retentionList;

    private static final long OWNER_USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final Long VIDEO_ID = 1L;
    private static final double DURATION = 600.0; // 10분

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .name("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", OWNER_USER_ID);

        Channel channel = Channel.builder()
                .user(user)
                .name("테스트채널")
                .build();

        video = Video.builder()
                .channel(channel)
                .title("테스트영상")
                .duration(DURATION)
                .publishedAt(LocalDateTime.now().minusDays(30))
                .build();

        retentionList = createRetentionList();
    }

    // 100개 retention 데이터 생성 (timeRatio 0.01~1.00, 균등 감소)
    // setUp()용 - 구간 구분 없이 0.5씩 균등 감소
    private List<AudienceRetention> createRetentionList() {
        List<AudienceRetention> list = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            list.add(AudienceRetention.builder()
                    .video(video)
                    .timeRatio(i * 0.01)
                    .retentionRate(100.0 - (i * 0.5))
                    .collectedAt(LocalDateTime.now())
                    .build());
        }
        return list;
    }

    // 구간별 다른 감소율로 retention 데이터 생성 (dropRate 검증 전용)
    // segmentSteps[i] = i번 구간의 연속 감소량
    // timeRatio는 이 테스트의 관심사가 아니므로 0.0으로 고정
    private List<AudienceRetention> createRetentionListWithSteps(double[] segmentSteps) {
        double[] segmentStartValues = {80.0, 30.0, 90.0, 15.0};
        List<AudienceRetention> list = new ArrayList<>();

        for (int seg = 0; seg < 4; seg++) {
            double rate = segmentStartValues[seg];
            for (int i = 0; i < 25; i++) {
                list.add(AudienceRetention.builder()
                        .video(video)
                        .retentionRate(rate)
                        .timeRatio(0.0)
                        .collectedAt(LocalDateTime.now())
                        .build());
                rate -= segmentSteps[seg];
            }
        }
        return list;
    }

    @Test
    void 이탈_구간_조회_성공_4개_구간_반환() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(retentionList);

        // when
        DropPointsResponse response = videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID);

        // then
        assertThat(response.dropPoints()).hasSize(4);
    }

    @Test
    void 이탈_구간_조회_성공_마지막_구간_endTime_null() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(retentionList);

        // when
        DropPointsResponse response = videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID);

        // then
        DropPointsResponse.DropPoint lastSegment = response.dropPoints().get(3);
        assertThat(lastSegment.endTime()).isNull();
    }

    @Test
    void 이탈_구간_조회_성공_처음_3개_구간_endTime_존재() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(retentionList);

        // when
        DropPointsResponse response = videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID);

        // then
        assertThat(response.dropPoints().get(0).endTime()).isNotNull();
        assertThat(response.dropPoints().get(1).endTime()).isNotNull();
        assertThat(response.dropPoints().get(2).endTime()).isNotNull();
    }

    @Test
    void 이탈_구간_조회_성공_startTime_오름차순() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(retentionList);

        // when
        DropPointsResponse response = videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID);

        // then
        // 각 구간의 startTime이 이전 구간보다 뒤여야 함
        // "m:ss" 문자열 비교 대신 인덱스 기반으로 구간 순서 검증
        List<DropPointsResponse.DropPoint> dropPoints = response.dropPoints();
        assertThat(dropPoints.get(0).startTime()).isEqualTo("0:06");  // index 0  → timeRatio 0.01 → 0.01 * 600 = 6s
        assertThat(dropPoints.get(1).startTime()).isEqualTo("2:36");  // index 25 → timeRatio 0.26 → 0.26 * 600 = 156s
        assertThat(dropPoints.get(2).startTime()).isEqualTo("5:06");  // index 50 → timeRatio 0.51 → 0.51 * 600 = 306s
        assertThat(dropPoints.get(3).startTime()).isEqualTo("7:36");  // index 75 → timeRatio 0.76 → 0.76 * 600 = 456s
    }

    @Test
    void 이탈_구간_조회_성공_구간별_평균_이탈률_검증() {
        // given
        // Segment 1: step 2.0 → avgDropRate = 2.0
        // Segment 2: step 1.0 → avgDropRate = 1.0
        // Segment 3: step 3.0 → avgDropRate = 3.0
        // Segment 4: step 0.5 → avgDropRate = 0.5
        List<AudienceRetention> customList = createRetentionListWithSteps(new double[]{2.0, 1.0, 3.0, 0.5});
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(customList);

        // when
        DropPointsResponse response = videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID);

        // then
        assertThat(response.dropPoints().get(0).dropRate()).isEqualTo(2.0);
        assertThat(response.dropPoints().get(1).dropRate()).isEqualTo(1.0);
        assertThat(response.dropPoints().get(2).dropRate()).isEqualTo(3.0);
        assertThat(response.dropPoints().get(3).dropRate()).isEqualTo(0.5);
    }

    @Test
    void 이탈_구간_조회_영상_없으면_예외() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.VIDEO_NOT_FOUND);
    }

    @Test
    void 이탈_구간_조회_소유자_불일치이면_예외() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));

        // when & then
        assertThatThrownBy(() -> videoService.getDropPoints(OTHER_USER_ID, VIDEO_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.AUTH_FORBIDDEN);
    }

    @Test
    void 이탈_구간_조회_retention_데이터_없으면_예외() {
        // given
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.RETENTION_NOT_FOUND);
    }

    @Test
    void 이탈_구간_조회_retention_데이터_100개_아니면_예외() {
        // given
        List<AudienceRetention> incompleteList = retentionList.subList(0, 50);
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(incompleteList);

        // when & then
        assertThatThrownBy(() -> videoService.getDropPoints(OWNER_USER_ID, VIDEO_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("error", ErrorDefine.RETENTION_INVALID);
    }

    @Test
    void 시청_지속률_조회_성공_감소량_0_05_이상이면_isDrop_true() {
        // given - 차이 0.06 (>= 0.05)
        List<AudienceRetention> list = List.of(
                AudienceRetention.builder().video(video).timeRatio(0.01).retentionRate(90.0).collectedAt(LocalDateTime.now()).build(),
                AudienceRetention.builder().video(video).timeRatio(0.02).retentionRate(89.94).collectedAt(LocalDateTime.now()).build()
        );
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(list);

        // when
        AudienceRetentionResponse response = videoService.getRetention(OWNER_EMAIL, VIDEO_ID);

        // then
        assertThat(response.retentionData().get(1).isDrop()).isTrue();
    }

    @Test
    void 시청_지속률_조회_성공_감소량_0_05_미만이면_isDrop_false() {
        // given - 차이 0.03 (< 0.05)
        List<AudienceRetention> list = List.of(
                AudienceRetention.builder().video(video).timeRatio(0.01).retentionRate(90.0).collectedAt(LocalDateTime.now()).build(),
                AudienceRetention.builder().video(video).timeRatio(0.02).retentionRate(89.97).collectedAt(LocalDateTime.now()).build()
        );
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(list);

        // when
        AudienceRetentionResponse response = videoService.getRetention(OWNER_EMAIL, VIDEO_ID);

        // then
        assertThat(response.retentionData().get(1).isDrop()).isFalse();
    }

    @Test
    void 시청_지속률_조회_성공_displayTime이_올바른_MM_SS_형식으로_반환() {
        // given - timeRatio 0.5 * duration 600.0 = 300s → "5:00"
        List<AudienceRetention> list = List.of(
                AudienceRetention.builder().video(video).timeRatio(0.5).retentionRate(80.0).collectedAt(LocalDateTime.now()).build()
        );
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(video));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(list);

        // when
        AudienceRetentionResponse response = videoService.getRetention(OWNER_EMAIL, VIDEO_ID);

        // then
        assertThat(response.retentionData().get(0).displayTime()).isEqualTo("5:00");
    }

    @Test
    void 시청_지속률_조회_duration_null이면_displayTime이_0_00_반환() {
        // given - duration이 null인 영상
        Video videoWithNullDuration = Video.builder()
                .channel(video.getChannel())
                .title("duration없는영상")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .build();
        List<AudienceRetention> list = List.of(
                AudienceRetention.builder().video(videoWithNullDuration).timeRatio(0.5).retentionRate(80.0).collectedAt(LocalDateTime.now()).build()
        );
        given(videoRepository.findById(VIDEO_ID)).willReturn(Optional.of(videoWithNullDuration));
        given(audienceRetentionRepository.findByVideoIdOrderByTimeRatioAsc(VIDEO_ID)).willReturn(list);

        // when
        AudienceRetentionResponse response = videoService.getRetention(OWNER_EMAIL, VIDEO_ID);

        // then
        assertThat(response.retentionData().get(0).displayTime()).isEqualTo("0:00");
    }
}
