package com.example.inflace.domain.youtubecategory.repository;

import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YoutubeCategoryRepository extends JpaRepository<YoutubeCategory, Long> {
    List<YoutubeCategory> findByYoutubeCategoryIdIn(List<Integer> youtubeCategoryIds);
}
