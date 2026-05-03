package com.example.inflace.domain.channel.domain;

import com.example.inflace.domain.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_bookmark", uniqueConstraints = @UniqueConstraint(
        name = "uk_channel_bookmark_channel_user",
        columnNames = {"channel_id", "user_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static ChannelBookmark of(Channel channel, User user) {
        ChannelBookmark channelBookmark = new ChannelBookmark();
        channelBookmark.channel = channel;
        channelBookmark.user = user;
        return channelBookmark;
    }
}
