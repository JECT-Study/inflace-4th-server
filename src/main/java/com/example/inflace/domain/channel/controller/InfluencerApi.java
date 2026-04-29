package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Influencer", description = "인플루언서 탐색 및 검색 API")
public interface InfluencerApi {

    @Operation(
            summary = "인플루언서 검색",
            description = """
                    조건 기반으로 인플루언서 목록을 Slice 방식으로 조회합니다.
                    
                    - 기본 정렬 기준: `engagement_rate`
                    - 기본 정렬 방향: `DESC`
                    - 기본 최소 참여율: `5.0`
                    - 기본 페이지 크기: `9`
                    - `categoryNames`는 동일한 쿼리 파라미터를 반복 전달합니다. 예: `?categoryNames=게임&categoryNames=엔터테인먼트`
                    - 커서 페이지네이션 사용 시 정렬 기준에 맞는 마지막 정렬값과 `lastChannelId`를 함께 전달해야 합니다.
                    - `sortCriteria=engagement_rate`일 때는 `lastEngagementSortRate` + `lastChannelId`를 사용합니다.
                    - `sortCriteria=subscriber`일 때는 `lastSubscriberSortCount` + `lastChannelId`를 사용합니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT})
    BaseResponse<SliceResponse<GetInfluencerSearchResponse>> getInfluencersWithSearchCondition(
            @ParameterObject InfluencerSearchCondition searchCondition
    );
}
