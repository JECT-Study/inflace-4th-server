package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.Video;
import com.example.inflace.domain.video.domain.VideoStats;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
    @Query("""
            select vs
            from VideoStats vs
            where vs.video.id in :videoIds
            """)
    List<VideoStats> findAllByVideoIds(@Param("videoIds") List<Long> videoIds);
           
    Optional<VideoStats> findByVideoId(Long videoId);

    Optional<VideoStats> findByVideo(Video video);
}
