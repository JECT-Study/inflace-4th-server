package com.example.inflace.domain.channel.domain;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_user_youtube",
                columnNames = {"user_id", "youtube_channel_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    @Column(name = "youtube_channel_id")
    private String youtubeChannelId;

    @Column(name = "channel_handle")
    private String channelHandle;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "uploads_playlist_id")
    private String uploadsPlaylistId;

    @Column(name = "youtube_published_at")
    private LocalDateTime youtubePublishedAt;

    @Builder
    public Channel(User user, String name, String youtubeChannelId, String channelHandle,
                   String profileImageUrl, String uploadsPlaylistId, LocalDateTime youtubePublishedAt) {
        this.user = user;
        this.name = name;
        this.youtubeChannelId = youtubeChannelId;
        this.channelHandle = channelHandle;
        this.profileImageUrl = profileImageUrl;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.youtubePublishedAt = youtubePublishedAt;
    }
}
