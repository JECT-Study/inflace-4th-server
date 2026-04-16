package com.example.inflace.domain.channel.domain;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.global.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    @Column(name = "youtube_channel_id")
    private String youtubeChannelId;

    @Type(value = StringArrayType.class)
    @Column(name = "category", columnDefinition = "text[]")
    private String[] category;

    @Column(name = "channel_handle")
    private String channelHandle;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Builder
    public Channel(User user, String name, String youtubeChannelId, String[] category, String channelHandle, String profileImage, LocalDateTime enteredAt) {
        this.user = user;
        this.name = name;
        this.youtubeChannelId = youtubeChannelId;
        this.category = category;
        this.channelHandle = channelHandle;
        this.profileImage = profileImage;
        this.enteredAt = enteredAt;
    }

    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    public void updateAll(String name, String profileImage, String youtubeChannelId, String channelHandle, String[] category, LocalDateTime enteredAt) {
        this.name = name;
        this.profileImage = profileImage;
        this.youtubeChannelId = youtubeChannelId;
        this.channelHandle = channelHandle;
        this.category = category;
        this.enteredAt = enteredAt;
    }
}
