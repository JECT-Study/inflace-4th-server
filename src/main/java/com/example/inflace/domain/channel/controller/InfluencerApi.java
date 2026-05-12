package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerBookmarksResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerInsightSummaryResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
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
                    조건 기반으로 인플루언서 목록을 cursor 기반으로 조회합니다.
                    
                    - 로그인한 사용자의 즐겨찾기 여부(`bookmarked`)가 각 인플루언서 항목에 함께 내려갑니다.
                    - 기본 정렬 기준: `engagement_rate`
                    - 기본 정렬 방향: `DESC`
                    - 기본 최소 참여율: `2.0`
                    - 기본 페이지 크기: `9`
                    - `categoryIds`는 `/api/v1/youtube-categories`에서 내려준 `id` 값을 반복 전달합니다. 예: `?categoryIds=1&categoryIds=2`
                    - 다음 페이지 요청 시에는 이전 응답의 `nextCursor` 값을 `cursor`로 그대로 전달합니다.
                    - 기존 검색 필터와 정렬 조건은 다음 페이지 요청에서도 동일하게 유지해야 합니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.INVALID_ARGUMENT, ErrorDefine.AUTHENTICATION_FAILED})
    BaseResponse<CursorSliceResponse<GetInfluencerSearchResponse>> getInfluencersWithSearchCondition(
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

    @Operation(
            summary = "인플루언서 채널 인사이트 조회",
            description = """
                    특정 인플루언서 채널의 팬층, 콘텐츠, 활동, 롱폼/숏폼 지표를 종합 조회합니다.
                    
                    - 영상이 50개 이상인 채널만 조회 가능합니다.
                    - 기본 채널 정보로 `channelName`, `categories`, `channelHandle`, `joinedAt`, `subscriberCount`를 함께 반환합니다.
                    - 이 API는 지표 조회 전용이며 OpenAI 요약 생성은 별도 API에서 수행합니다.
                    - 조회한 인사이트 계산값은 Redis에 캐시되며 AI 요약 API에서 재사용될 수 있습니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.CHANNEL_INSIGHT_REQUIRES_MIN_VIDEO_COUNT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<GetInfluencerInsightResponse> getInfluencerInsight(
            @Parameter(description = "인사이트를 조회할 채널 ID", example = "42")
            @PathVariable Long channelId
    );

    @Operation(
            summary = "인플루언서 채널 인사이트 AI 요약 조회",
            description = """
                    특정 인플루언서 채널의 인사이트 AI 요약만 조회합니다.
                    
                    - 프론트는 일반적으로 인사이트 API 호출 후 이 API를 호출하는 흐름을 사용합니다.
                    - 채널 description, 최신 영상 description 10개, 인사이트 지표를 바탕으로 LLM 요약을 생성합니다.
                    - 먼저 Redis에서 요약 캐시를 조회하고, 없으면 인사이트 계산 캐시를 재사용합니다.
                    - 인사이트 계산 캐시가 없으면 직접 인사이트를 다시 계산한 뒤 요약을 생성합니다.
                    - 생성된 요약은 Redis에 6시간 동안 캐시됩니다.
                    - OpenAI 호출에 실패하면 `summary`는 null로 반환될 수 있습니다.
                    """
    )
    @ApiErrorDefines({ErrorDefine.CHANNEL_INSIGHT_REQUIRES_MIN_VIDEO_COUNT, ErrorDefine.CHANNEL_NOT_FOUND})
    BaseResponse<GetInfluencerInsightSummaryResponse> getInfluencerInsightSummary(
            @Parameter(description = "AI 요약을 조회할 채널 ID", example = "42")
            @PathVariable Long channelId
    );
}
