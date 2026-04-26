package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.VideoAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoAnalyticsRepository extends JpaRepository<VideoAnalytics, Long> {
    Optional<VideoAnalytics> findByVideoId(Long videoId);

    List<VideoAnalytics> findAllByVideoIdIn(List<Long> videoIds);
}
