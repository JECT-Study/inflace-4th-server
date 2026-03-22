package com.example.inflace.domain.video.domain;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    private Double duration;

    @Column(name = "is_short")
    private boolean isShort;

    @Column(name = "rising_score")
    private int risingScore;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "hashtags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] hashtags;

    @Builder
    public Video(Channel channel, String title, String thumbnailUrl, Double duration, boolean isShort, int risingScore,
                 LocalDateTime publishedAt, String[] hashtags) {
        this.channel = channel;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.isShort = isShort;
        this.risingScore = risingScore;
        this.publishedAt = publishedAt;
        this.hashtags = hashtags;
    }
}
