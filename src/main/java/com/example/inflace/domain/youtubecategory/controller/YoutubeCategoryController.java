package com.example.inflace.domain.youtubecategory.controller;

import com.example.inflace.domain.youtubecategory.dto.response.GetYoutubeCategoriesResponse;
import com.example.inflace.domain.youtubecategory.service.YoutubeCategoryService;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/youtube-categories")
@RequiredArgsConstructor
public class YoutubeCategoryController implements YoutubeCategoryApi {

    private final YoutubeCategoryService youtubeCategoryService;

    @Override
    @GetMapping("")
    public BaseResponse<GetYoutubeCategoriesResponse> getYoutubeCategories() {
        return new BaseResponse<>(youtubeCategoryService.getYoutubeCategories());
    }
}
