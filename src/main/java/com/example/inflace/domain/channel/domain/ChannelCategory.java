package com.example.inflace.domain.channel.domain;

import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;
import com.example.inflace.global.entity.BaseTimeEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "channel_category",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_category",
                columnNames = {"channel_id", "category_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelCategory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private YoutubeCategory category;

    public static ChannelCategory of(Channel channel, YoutubeCategory category) {
        ChannelCategory relation = new ChannelCategory();
        relation.channel = channel;
        relation.category = category;
        return relation;
    }
}
