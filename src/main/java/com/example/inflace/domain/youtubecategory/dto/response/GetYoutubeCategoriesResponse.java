package com.example.inflace.domain.youtubecategory.dto.response;

import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;

import java.util.List;

public record GetYoutubeCategoriesResponse(
        List<YoutubeCategoryOutput> youtubeCategories
) {
    public record YoutubeCategoryOutput(
            Long id,
            String title
    ) {
        public static YoutubeCategoryOutput from(YoutubeCategory youtubeCategory) {
            return new YoutubeCategoryOutput(
                    youtubeCategory.getId(),
                    youtubeCategory.getTitle()
            );
        }
    }
}
