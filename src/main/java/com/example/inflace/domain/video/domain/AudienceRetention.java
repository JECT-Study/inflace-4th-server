package com.example.inflace.domain.video.domain;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audience_retention")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AudienceRetention extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audience_retention_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "time_ratio")
    private Double timeRatio;

    @Column(name = "retention_rate")
    private Double retentionRate;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Builder
    public AudienceRetention(Video video, Double timeRatio, Double retentionRate, LocalDateTime collectedAt) {
        this.video = video;
        this.timeRatio = timeRatio;
        this.retentionRate = retentionRate;
        this.collectedAt = collectedAt;
    }
}
