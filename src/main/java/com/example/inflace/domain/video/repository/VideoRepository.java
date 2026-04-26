package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.Video;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Query("""
            select v
            from Video v
            left join VideoStats vs on vs.video = v
            left join VideoAnalytics va on va.video = v
            where v.channel.id = :channelId
              and v.isShort = :isShort
            order by coalesce(vs.risingScore, 0) desc, coalesce(va.ctr, 0) desc, v.id desc
            """)
    List<Video> findTopVideos(
            @Param("channelId") Long channelId,
            @Param("isShort") boolean isShort,
            Pageable pageable
    );

    @Query("""
       select v
       from Video v
       left join VideoAnalytics va on va.video = v
       where v.channel.id = :channelId
       order by coalesce(va.unsubscribedViewerPercentage, 0) desc, v.id desc
    """)
    List<Video> findTopNewSubscriberVideos(
            @Param("channelId") Long channelId,
            Pageable pageable
    );

    @Query("""
            select v
            from Video v
            left join VideoStats vs on vs.video = v
            left join VideoAnalytics va on va.video = v
            where v.channel.id = :channelId
            order by coalesce(vs.risingScore, 0) desc, coalesce(va.ctr, 0) desc, v.id desc
            """)
    List<Video> findAllTopVideos(@Param("channelId") Long channelId, Limit limit);

    Long countByChannelIdAndPublishedAtGreaterThanEqual(Long channelId, LocalDateTime publishedAt);

    List<Video> findByChannelId(Long channelId);
}
