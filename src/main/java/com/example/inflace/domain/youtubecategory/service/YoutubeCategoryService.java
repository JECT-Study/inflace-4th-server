package com.example.inflace.domain.youtubecategory.service;

import com.example.inflace.domain.youtubecategory.domain.YoutubeCategory;
import com.example.inflace.domain.youtubecategory.dto.response.GetYoutubeCategoriesResponse;
import com.example.inflace.domain.youtubecategory.repository.YoutubeCategoryRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeCategoryService {

    private final YoutubeCategoryRepository youtubeCategoryRepository;

    @ReadOnlyTransactional
    public GetYoutubeCategoriesResponse getYoutubeCategories() {
        List<YoutubeCategory> youtubeCategories = youtubeCategoryRepository.findByAssignableIsTrue();
        return new GetYoutubeCategoriesResponse(
                youtubeCategories.stream()
                        .map(GetYoutubeCategoriesResponse.YoutubeCategoryOutput::from)
                        .toList()
        );
    }
}
