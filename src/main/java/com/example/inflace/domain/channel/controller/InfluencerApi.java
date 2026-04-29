package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerBookmarksResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Influencer", description = "인플루언서 탐색 및 검색 API")
public interface InfluencerApi {

    @Operation(
            summary = "인플루언서 검색",
            description = """
                    조건 기반으로 인플루언서 목록을 Slice 방식으로 조회합니다.
                    
                    - 로그인한 사용자의 즐겨찾기 여부(`bookmarked`)가 각 인플루언서 항목에 함께 내려갑니다.
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
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.AUTHENTICATION_FAILED})
    BaseResponse<SliceResponse<GetInfluencerSearchResponse>> getInfluencersWithSearchCondition(
            @ParameterObject InfluencerSearchCondition searchCondition
    );

    @Operation(
            summary = "인플루언서 즐겨찾기 추가",
            description = """
                    현재 로그인한 사용자의 즐겨찾기 목록에 특정 인플루언서를 추가합니다.
                    
                    - 이미 즐겨찾기한 채널에 중복 요청하는 경우 DB unique 제약조건에 의해 실패할 수 있습니다.
                    - 성공 시 응답 본문은 비어 있습니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.AUTHENTICATION_FAILED, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<Void> createChannelBookmark(
            @Parameter(description = "즐겨찾기할 채널 ID", example = "42")
            @PathVariable Long channelId
    );

    @Operation(
            summary = "인플루언서 즐겨찾기 삭제",
            description = """
                    현재 로그인한 사용자의 즐겨찾기 목록에서 특정 인플루언서를 제거합니다.
                    
                    - 즐겨찾기 관계가 없어도 요청은 정상적으로 종료될 수 있습니다.
                    - 성공 시 응답 본문은 비어 있습니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.AUTHENTICATION_FAILED, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<Void> deleteChannelBookmark(
            @Parameter(description = "즐겨찾기 해제할 채널 ID", example = "42")
            @PathVariable Long channelId
    );

    @Operation(
            summary = "내 인플루언서 즐겨찾기 목록 조회",
            description = """
                    현재 로그인한 사용자가 즐겨찾기한 인플루언서의 채널 ID 목록을 조회합니다.
                    
                    - 응답은 채널 ID 배열만 반환합니다.
                    - 인플루언서 목록 API의 `bookmarked` 상태와 함께 사용할 수 있습니다.
                    """
    )
    @ApiErrorDefines(ErrorDefine.AUTHENTICATION_FAILED)
    BaseResponse<GetInfluencerBookmarksResponse> getInfluencerBookmarks();
}
