package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
