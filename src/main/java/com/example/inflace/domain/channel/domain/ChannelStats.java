package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_stats_channel",
                columnNames = "channel_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelStats extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "total_video_count")
    private Long totalVideoCount;

    @Column(name = "recent_upload_count_30d")
    private Integer recentUploadCount30d;

    @Column(name = "avg_views_recent_n")
    private Double avgViewsRecentN;

    @Column(name = "avg_engagement_rate_recent_n")
    private Double avgEngagementRateRecentN;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Builder
    public ChannelStats(Channel channel, Long subscriberCount, Long totalViewCount, Long totalVideoCount,
                        Integer recentUploadCount30d, Double avgViewsRecentN,
                        Double avgEngagementRateRecentN, LocalDateTime collectedAt) {
        this.channel = channel;
        this.subscriberCount = subscriberCount;
        this.totalViewCount = totalViewCount;
        this.totalVideoCount = totalVideoCount;
        this.recentUploadCount30d = recentUploadCount30d;
        this.avgViewsRecentN = avgViewsRecentN;
        this.avgEngagementRateRecentN = avgEngagementRateRecentN;
        this.collectedAt = collectedAt;
    }

    public Double getAvgEngagementRate() {
        return avgEngagementRateRecentN;
    }
}
