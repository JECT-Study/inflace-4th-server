package com.example.inflace.domain.brandcollaboration.controller;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "BrandCollaboration", description = "경쟁사 협업 인플루언서 조회 API")
public interface BrandCollaborationApi {

    @Operation(
            summary = "브랜드 협업 영상 검색",
            description = """
                    브랜드명으로 YouTube PPL/협찬 영상 및 크리에이터 목록을 조회합니다.

                    - `videoPaidProductPlacement=true` 기반으로 유료 광고 영상을 필터링합니다.
                    - 기본 정렬 기준: `LATEST`, 기본 정렬 방향: `DESC`, 기본 페이지 크기: `9`
                    - `includeKeywords`, `excludeKeywords`는 동일한 쿼리 파라미터를 반복 전달합니다.
                    - 포함 키워드와 제외 키워드가 중복될 경우 400 에러가 반환됩니다.
                    - 제외 키워드는 최대 5개까지 설정할 수 있습니다.
                    - 다음 페이지 요청 시 이전 응답의 `nextCursor`를 `cursor`로 전달합니다.
                    - 기존 검색 필터와 정렬 조건은 다음 페이지 요청에서도 동일하게 유지해야 합니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<CursorSliceResponse<BrandCollaborationVideoResponse>> search(
            @ParameterObject BrandCollaborationSearchCondition condition
    );
}
