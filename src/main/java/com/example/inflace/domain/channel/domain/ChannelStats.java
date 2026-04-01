package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

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

    @Column(name = "subscriber_view_count")
    private Long subscriberViewCount;

    @Column(name = "avg_engagement_rate")
    private Double avgEngagementRate;

    @Type(JsonBinaryType.class)
    @Column(name = "audience_gender", columnDefinition = "jsonb")
    private Map<String, Double> audienceGender;

    @Type(JsonBinaryType.class)
    @Column(name = "audience_age", columnDefinition = "jsonb")
    private Map<String, Double> audienceAge;

    @Type(JsonBinaryType.class)
    @Column(name = "audience_country", columnDefinition = "jsonb")
    private Map<String, Double> audienceCountry;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Builder
    public ChannelStats(Channel channel, Long subscriberCount, Long totalViewCount, Long subscriberViewCount,
                        Double avgEngagementRate, Map<String, Double> audienceGender, Map<String, Double> audienceAge,
                        Map<String, Double> audienceCountry, LocalDateTime collectedAt) {
        this.channel = channel;
        this.subscriberCount = subscriberCount;
        this.totalViewCount = totalViewCount;
        this.subscriberViewCount = subscriberViewCount;
        this.avgEngagementRate = avgEngagementRate;
        this.audienceGender = audienceGender;
        this.audienceAge = audienceAge;
        this.audienceCountry = audienceCountry;
        this.collectedAt = collectedAt;
    }
}
