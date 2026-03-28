package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.VideoStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
}
