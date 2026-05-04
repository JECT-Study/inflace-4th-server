package com.example.inflace.domain.video.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_video_stats_video",
                columnNames = "video_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoStats extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "vph", nullable = false)
    private Double vph = 0.0;

    @Column(name = "outlier_score", nullable = false)
    private Double outlierScore = 0.0;

    @Column(name = "rising_score")
    private Double risingScore;

    @Builder
    public VideoStats(Video video, Long viewCount, Long likeCount, Long commentCount,
                      Double vph, Double outlierScore, Double risingScore, LocalDateTime collectedAt) {
        this.video = video;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.vph = vph;
        this.outlierScore = outlierScore;
        this.risingScore = risingScore;
        this.collectedAt = collectedAt;
    }

    public void update(Long viewCount, Long likeCount, Long commentCount, LocalDateTime collectedAt) {
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.collectedAt = collectedAt;
    }
}
