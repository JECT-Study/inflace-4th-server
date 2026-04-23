package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel_stats_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_stats_history_channel_date",
                columnNames = {"channel_id", "snapshot_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelStatsHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public ChannelStatsHistory(Channel channel, LocalDate snapshotDate, Long subscriberCount,
                               Long totalViewCount, Long videoCount, LocalDateTime collectedAt) {
        this.channel = channel;
        this.snapshotDate = snapshotDate;
        this.subscriberCount = subscriberCount;
        this.totalViewCount = totalViewCount;
        this.videoCount = videoCount;
        this.collectedAt = collectedAt;
    }
}
