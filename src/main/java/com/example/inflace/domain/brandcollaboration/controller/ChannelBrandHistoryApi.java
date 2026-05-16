package com.example.inflace.domain.brandcollaboration.controller;

import com.example.inflace.domain.brandcollaboration.dto.request.ChannelBrandHistorySearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryAnalysisResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryVideoResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "ChannelBrandHistory", description = "채널 브랜드 협업 이력 API")
public interface ChannelBrandHistoryApi {

    @Operation(
            summary = "채널 PPL 영상 목록 조회",
            description = """
                    특정 채널의 유료 광고(PPL) 영상 목록을 조회합니다.

                    - `channelId`는 Path Variable로 전달합니다. (YouTube 채널 ID)
                    - `videoPaidProductPlacement=true` 기반으로 유료 광고 영상을 필터링합니다.
                    - 기본 정렬 기준: `LATEST`, 기본 정렬 방향: `DESC` (ASC 미지원), 기본 페이지 크기: `9`
                    - 다음 페이지 요청 시 이전 응답의 `nextCursor`를 `cursor`로 전달합니다.
                    - `brands`는 영상 설명(`snippet.description`) 파싱 후 브랜드 alias 매핑 결과입니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<CursorSliceResponse<ChannelBrandHistoryVideoResponse>> search(
            @Parameter(description = "YouTube 채널 ID", example = "UCxxxxxxxxxxxxxxxxxxxxxx")
            @PathVariable String channelId,
            @ParameterObject ChannelBrandHistorySearchCondition condition
    );

    @Operation(
            summary = "채널 PPL 영상 집계 분석",
            description = """
                    특정 채널의 PPL 영상(최대 50개)을 집계하여 분석 결과를 반환합니다.

                    - `channelId`는 Path Variable로 전달합니다. (YouTube 채널 ID)

                    **brands** (브랜드별 협업 수)
                    - 전체 영상의 태그를 집계하여 빈도순 정렬
                    - 동일 태그(브랜드)가 여러 영상에 등장하면 count로 재협업 횟수 확인 가능

                    **contentTypeDistribution** (콘텐츠 유형 분포)
                    - LONG_FORM / SHORT_FORM 비율 (영상 길이 180초 기준)

                    **categoryDistribution** (카테고리 분포)
                    - 영상 categoryId → YoutubeCategory DB 룩업 후 비율 계산

                    **avgViewsByContentType** (유형별 평균 조회수)
                    - LONG_FORM / SHORT_FORM 각 평균 viewCount
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<ChannelBrandHistoryAnalysisResponse> analysis(
            @Parameter(description = "YouTube 채널 ID", example = "UCxxxxxxxxxxxxxxxxxxxxxx")
            @PathVariable String channelId,
            @ParameterObject ChannelBrandHistorySearchCondition condition
    );
}
