package com.example.inflace.domain.youtubecategory.controller;

import com.example.inflace.domain.youtubecategory.dto.response.GetYoutubeCategoriesResponse;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "YoutubeCategory", description = "유튜브 카테고리 API")
public interface YoutubeCategoryApi {

    @Operation(
            summary = "유튜브 카테고리 목록 조회",
            description = "인플루언서 검색 필터에 사용할 유튜브 카테고리 목록을 조회합니다."
    )
    BaseResponse<GetYoutubeCategoriesResponse> getYoutubeCategories();
}
