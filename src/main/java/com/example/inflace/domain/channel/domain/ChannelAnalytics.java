package com.example.inflace.domain.channel.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "channel_analytics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_analytics_channel",
                columnNames = "channel_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelAnalytics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_analytics_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "views")
    private Long views;

    @Column(name = "subscriber_view_count")
    private Long subscriberViewCount;

    @Column(name = "non_subscriber_view_count")
    private Long nonSubscriberViewCount;

    @Column(name = "watched_minutes")
    private Long watchedMinutes;

    @Column(name = "average_view_duration_seconds")
    private Integer averageViewDurationSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_gender", columnDefinition = "jsonb")
    private Map<String, Double> audienceGender;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_age", columnDefinition = "jsonb")
    private Map<String, Double> audienceAge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_country", columnDefinition = "jsonb")
    private Map<String, Double> audienceCountry;

    @Builder
    public ChannelAnalytics(Channel channel, LocalDate startDate, LocalDate endDate, LocalDateTime collectedAt,
                            Long views, Long subscriberViewCount, Long nonSubscriberViewCount, Long watchedMinutes,
                            Integer averageViewDurationSeconds, Map<String, Double> audienceGender,
                            Map<String, Double> audienceAge, Map<String, Double> audienceCountry) {
        this.channel = channel;
        this.startDate = startDate;
        this.endDate = endDate;
        this.collectedAt = collectedAt;
        this.views = views;
        this.subscriberViewCount = subscriberViewCount;
        this.nonSubscriberViewCount = nonSubscriberViewCount;
        this.watchedMinutes = watchedMinutes;
        this.averageViewDurationSeconds = averageViewDurationSeconds;
        this.audienceGender = audienceGender;
        this.audienceAge = audienceAge;
        this.audienceCountry = audienceCountry;
    }
}
