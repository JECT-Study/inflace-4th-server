package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.dto.VideoStatsChannelAverages;
import com.example.inflace.domain.video.domain.VideoStats;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
    List<VideoStats> findAllByVideoIdIn(List<Long> videoIds);
           
    Optional<VideoStats> findByVideoId(Long videoId);

    @Query("""
            select new com.example.inflace.domain.video.dto.VideoStatsChannelAverages(
                avg(coalesce(vs.viewCount, 0)),
                avg(coalesce(vs.likeCount, 0)),
                avg(coalesce(vs.commentCount, 0)),
                avg(coalesce(va.shareCount, 0)),
                avg(coalesce(va.subscribersGained, 0)),
                avg(coalesce(va.avgWatchDuration, 0.0)),
                avg(case
                    when coalesce(vs.viewCount, 0) = 0 then 0.0
                    else ((coalesce(vs.likeCount, 0) + coalesce(vs.commentCount, 0)) * 100.0 / vs.viewCount)
                end),
                avg(case
                    when coalesce(vs.viewCount, 0) = 0 then 0.0
                    else (coalesce(va.unsubscribedViewCount, 0) * 100.0 / vs.viewCount)
                end),
                avg(coalesce(vs.outlierScore, 0.0)),
                avg(coalesce(vs.vph, 0.0))
            )
            from Video v
            left join VideoStats vs on vs.video = v
            left join VideoAnalytics va on va.video = v
            where v.channel.id = :channelId
            """)
    VideoStatsChannelAverages findChannelAverages(@Param("channelId") Long channelId);

}
