package com.example.inflace.domain.brandcollaboration.controller;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationTrendsRequest;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationTrendsResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "BrandCollaboration", description = "경쟁사 협업 인플루언서 조회 API")
public interface BrandCollaborationApi {

    @Operation(
            summary = "브랜드 협업 영상 검색",
            description = """
                    포함 키워드로 YouTube PPL/협찬 영상 및 크리에이터 목록을 조회합니다.

                    - `videoPaidProductPlacement=true` 기반으로 유료 광고 영상을 필터링합니다.
                    - `includeKeywords`는 필수이며, 동일한 쿼리 파라미터를 반복 전달합니다. (최대 5개)
                    - `excludeKeywords`는 동일한 쿼리 파라미터를 반복 전달합니다. (최대 5개)
                    - 포함 키워드와 제외 키워드가 중복될 경우 400 에러가 반환됩니다.
                    - 기본 정렬 기준: `LATEST`, 기본 정렬 방향: `DESC`, 기본 페이지 크기: `9`
                    - 다음 페이지 요청 시 이전 응답의 `nextCursor`를 `cursor`로 전달합니다.
                    - 기존 검색 필터와 정렬 조건은 다음 페이지 요청에서도 동일하게 유지해야 합니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<CursorSliceResponse<BrandCollaborationVideoResponse>> search(
            @ParameterObject BrandCollaborationSearchCondition condition
    );

    @Operation(
            summary = "경쟁사 콘텐츠 트렌드 분석",
            description = """
                    선택한 YouTube 영상들을 AI로 분석하여 공통 키워드, 카테고리 분포, 전략 인사이트를 반환합니다.
                    채널 구독자 통계는 YouTube API 데이터를 기반으로 백엔드에서 직접 계산합니다.

                    - `commonKeywords`: 영상의 50% 이상에서 등장한 키워드, 최대 10개
                    - `keywordSummary`: 공통 키워드 트렌드 요약 (한국어)
                    - `categoryDistribution`: 영상 컨텐츠 기반 카테고리 분포 (AI 추론)
                    - `strategyInsight.pplIntent`: 해당 채널들을 PPL로 선택한 이유 분석
                    - `strategyInsight.competitivePoints`: 경쟁사 PPL 콘텐츠의 공통 강조 포인트
                    - `channelStats`: 협업 채널 수, 평균/범위 구독자 수, 평균 업로드 주기 (백엔드 계산)
                    - OpenAI 호출 실패 시 `commonKeywords`는 빈 리스트, AI 필드는 null, `channelStats`는 정상 반환됩니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<BrandCollaborationTrendsResponse> analyzeTrends(
            @RequestBody BrandCollaborationTrendsRequest request
    );
}
