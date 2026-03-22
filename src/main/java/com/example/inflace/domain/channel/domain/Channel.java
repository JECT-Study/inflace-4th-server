package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private User user;  // login merge 후에 user entity와 연동 필요

    private String name;

    @Column(name = "youtube_channel_id")
    private String youtubeChannelId;

    private String category;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Builder
    public Channel(User user, String name, String youtubeChannelId, String category, LocalDateTime enteredAt) {
        this.user = user;
        this.name = name;
        this.youtubeChannelId = youtubeChannelId;
        this.category = category;
        this.enteredAt = enteredAt;
    }
}
