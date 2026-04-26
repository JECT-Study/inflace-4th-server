package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {
    List<VideoTag> findAllByVideoId(Long videoId);
}
