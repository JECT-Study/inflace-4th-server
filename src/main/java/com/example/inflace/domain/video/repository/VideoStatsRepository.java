package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.VideoStats;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
    List<VideoStats> findAllByVideoIdIn(List<Long> videoIds);
           
    Optional<VideoStats> findByVideoId(Long videoId);

}
