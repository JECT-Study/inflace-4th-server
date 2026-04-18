package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_stats_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelStatsHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_stats_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "recorded_date")
    private LocalDateTime recordedDate;

    @Builder
    public ChannelStatsHistory(Channel channel, Long subscriberCount, LocalDateTime recordedDate) {
        this.channel = channel;
        this.subscriberCount = subscriberCount;
        this.recordedDate = recordedDate;
    }
}
