package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerBookmarksResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.domain.channel.service.InfluencerService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import com.example.inflace.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/influencers")
@RequiredArgsConstructor
public class InfluencerController implements InfluencerApi {

    private final InfluencerService influencerService;

    @Override
    @GetMapping
    public BaseResponse<CursorSliceResponse<GetInfluencerSearchResponse>> getInfluencersWithSearchCondition(
            @ModelAttribute InfluencerSearchCondition searchCondition
    ) {
        return new BaseResponse<>(influencerService.getInfluencersWithSearchCondition(
                searchCondition,
                SecurityUtils.getAuthenticatedUserId()
        ));
    }

    @PostMapping("/{channelId}/bookmark")
    public BaseResponse<Void> createChannelBookmark(
            @PathVariable Long channelId
    ) {
        influencerService.createChannelBookmark(channelId, SecurityUtils.getAuthenticatedUserId());
        return new BaseResponse<>(null);
    }

    @DeleteMapping("/{channelId}/bookmark")
    public BaseResponse<Void> deleteChannelBookmark(
            @PathVariable Long channelId
    ) {
        influencerService.deleteChannelBookmark(channelId, SecurityUtils.getAuthenticatedUserId());
        return new BaseResponse<>(null);
    }

    @GetMapping("/bookmarks")
    public BaseResponse<GetInfluencerBookmarksResponse> getInfluencerBookmarks() {
        return new BaseResponse<>(influencerService.getInfluencerBookmarks(SecurityUtils.getAuthenticatedUserId()));
    }
}
