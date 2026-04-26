package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "subscriber_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subscriber_log_channel_date",
                columnNames = {"channel_id", "recorded_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriberLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscriber_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "subscribers_gained")
    private Long subscribersGained;

    @Column(name = "subscribers_lost")
    private Long subscribersLost;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Builder
    public SubscriberLog(Channel channel, Long subscriberCount, Long subscribersGained, Long subscribersLost,
                         LocalDate recordedDate) {
        this.channel = channel;
        this.subscriberCount = subscriberCount;
        this.subscribersGained = subscribersGained;
        this.subscribersLost = subscribersLost;
        this.recordedDate = recordedDate;
    }
}
