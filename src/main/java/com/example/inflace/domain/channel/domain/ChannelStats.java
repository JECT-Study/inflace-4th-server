package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelStats extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_stats_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "avg_engagement_rate")
    private Double avgEngagementRate;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Builder
    public ChannelStats(Channel channel, Long subscriberCount, Long totalViewCount, Double avgEngagementRate, LocalDateTime collectedAt) {
        this.channel = channel;
        this.subscriberCount = subscriberCount;
        this.totalViewCount = totalViewCount;
        this.avgEngagementRate = avgEngagementRate;
        this.collectedAt = collectedAt;
    }
}
