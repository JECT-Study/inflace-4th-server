package com.example.inflace.domain.video.domain;

import com.example.inflace.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoStats extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_stats_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "comment_count")
    private Long commentCount;

    @Column(name = "share_count")
    private Long shareCount;

    private Double ctr;

    @Column(name = "avg_watch_duration")
    private Double avgWatchDuration;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "subscribers_gained")
    private Long subscribersGained;

    @Column(name = "unsubscribed_view_count")
    private Long unsubscribedViewCount;

    @Column(name = "average_view_percentage")
    private Double averageViewPercentage;

    @Column(name = "relative_retention_performance")
    private Double relativeRetentionPerformance;

    @Column(name = "unsubscribed_viewer_percentage")
    private Double unsubscribedViewerPercentage;

    @Builder
    public VideoStats(Video video, Long viewCount, Long likeCount, Long commentCount, Long shareCount, Double ctr,
                      Double avgWatchDuration, LocalDateTime collectedAt, Long subscribersGained,
                      Long unsubscribedViewCount, Double averageViewPercentage, Double relativeRetentionPerformance,
                      Double unsubscribedViewerPercentage) {
        this.video = video;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.ctr = ctr;
        this.avgWatchDuration = avgWatchDuration;
        this.collectedAt = collectedAt;
        this.subscribersGained = subscribersGained;
        this.unsubscribedViewCount = unsubscribedViewCount;
        this.averageViewPercentage = averageViewPercentage;
        this.relativeRetentionPerformance = relativeRetentionPerformance;
        this.unsubscribedViewerPercentage = unsubscribedViewerPercentage;
    }
}
