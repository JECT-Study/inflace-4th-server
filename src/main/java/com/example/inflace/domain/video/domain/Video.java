package com.example.inflace.domain.video.domain;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.YoutubeCategory;
import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_video_youtube_video",
                columnNames = "youtube_video_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_short")
    private boolean isShort;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "youtube_video_id")
    private String youtubeVideoId;

    private String description;

    @Column(name = "is_advertisement")
    private boolean isAdvertisement;

    @Builder
    public Video(Channel channel, Integer categoryId, String youtubeVideoId, String title, String description,
                 String thumbnailUrl, Integer durationSeconds, boolean isShort, boolean isAdvertisement,
                 LocalDateTime publishedAt) {
        this.channel = channel;
        this.categoryId = categoryId;
        this.youtubeVideoId = youtubeVideoId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.isShort = isShort;
        this.isAdvertisement = isAdvertisement;
        this.publishedAt = publishedAt;
    }

    @Transient
    public String getVideoUrl() {
        return youtubeVideoId != null ? "https://www.youtube.com/watch?v=" + youtubeVideoId : null;
    }

}
