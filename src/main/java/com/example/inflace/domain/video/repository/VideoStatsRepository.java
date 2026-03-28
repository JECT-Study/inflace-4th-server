package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.VideoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
    Optional<VideoStats> findByVideoId(Long videoId);
}
