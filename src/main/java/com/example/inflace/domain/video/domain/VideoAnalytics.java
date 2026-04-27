package com.example.inflace.domain.video.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_analytics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_video_analytics_video",
                columnNames = "video_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoAnalytics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_analytics_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "share_count")
    private Long shareCount;

    @Column(name = "ctr")
    private Double ctr;

    @Column(name = "avg_watch_duration")
    private Double avgWatchDuration;

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

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Builder
    public VideoAnalytics(Video video, Long shareCount, Double ctr, Double avgWatchDuration,
                          Long subscribersGained, Long unsubscribedViewCount, Double averageViewPercentage,
                          Double relativeRetentionPerformance, Double unsubscribedViewerPercentage,
                          LocalDateTime collectedAt) {
        this.video = video;
        this.shareCount = shareCount;
        this.ctr = ctr;
        this.avgWatchDuration = avgWatchDuration;
        this.subscribersGained = subscribersGained;
        this.unsubscribedViewCount = unsubscribedViewCount;
        this.averageViewPercentage = averageViewPercentage;
        this.relativeRetentionPerformance = relativeRetentionPerformance;
        this.unsubscribedViewerPercentage = unsubscribedViewerPercentage;
        this.collectedAt = collectedAt;
    }

    public void update(Long shareCount, Long subscribersGained, Double ctr, Double avgWatchDuration,
                       Double averageViewPercentage, LocalDateTime collectedAt) {
        this.shareCount = shareCount;
        this.subscribersGained = subscribersGained;
        this.ctr = ctr;
        this.avgWatchDuration = avgWatchDuration;
        this.averageViewPercentage = averageViewPercentage;
        this.collectedAt = collectedAt;
    }

    public void updateRelativeRetention(Double relativeRetentionPerformance) {
        this.relativeRetentionPerformance = relativeRetentionPerformance;
    }

    public void updateUnsubscribed(Long unsubscribedViewCount, Double unsubscribedViewerPercentage) {
        this.unsubscribedViewCount = unsubscribedViewCount;
        this.unsubscribedViewerPercentage = unsubscribedViewerPercentage;
    }
}
