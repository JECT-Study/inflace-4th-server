package com.example.inflace.domain.channel.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "youtube_category",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_youtube_category_youtube_category_id",
                columnNames = "youtube_category_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoutubeCategory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "youtube_category_id", nullable = false)
    private Integer youtubeCategoryId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "assignable")
    private Boolean assignable;
}
