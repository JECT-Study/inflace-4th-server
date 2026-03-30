package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.AudienceRetention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AudienceRetentionRepository extends JpaRepository<AudienceRetention, Long> {
    List<AudienceRetention> findByVideoIdOrderByTimeRatioAsc(Long videoId);
}
