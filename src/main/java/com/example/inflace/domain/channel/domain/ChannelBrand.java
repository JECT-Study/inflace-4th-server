package com.example.inflace.domain.channel.domain;

import com.example.inflace.domain.brand.domain.Brand;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "channel_brand",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_brand",
                columnNames = {"channel_id", "brand_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelBrand extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "matched_alias", nullable = false)
    private String matchedAlias;

    @Column(name = "source_youtube_video_id", nullable = true)
    private String sourceYoutubeVideoId;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    public static ChannelBrand of(Channel channel, Brand brand, String matchedAlias) {
        ChannelBrand relation = new ChannelBrand();
        relation.channel = channel;
        relation.brand = brand;
        relation.matchedAlias = matchedAlias;
        return relation;
    }
}
