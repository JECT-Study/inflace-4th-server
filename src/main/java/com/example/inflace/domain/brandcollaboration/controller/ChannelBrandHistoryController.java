package com.example.inflace.domain.brandcollaboration.controller;

import com.example.inflace.domain.brandcollaboration.dto.request.ChannelBrandHistorySearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryAnalysisResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.ChannelBrandHistoryVideoResponse;
import com.example.inflace.domain.brandcollaboration.service.ChannelBrandHistoryService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brand-collaborations/channels")
@RequiredArgsConstructor
public class ChannelBrandHistoryController implements ChannelBrandHistoryApi {

    private final ChannelBrandHistoryService channelBrandHistoryService;

    @Override
    @GetMapping("/{channelId}")
    public BaseResponse<CursorSliceResponse<ChannelBrandHistoryVideoResponse>> search(
            @PathVariable Long channelId,
            @ModelAttribute ChannelBrandHistorySearchCondition condition
    ) {
        return new BaseResponse<>(channelBrandHistoryService.search(channelId, condition));
    }

    @Override
    @GetMapping("/{channelId}/analysis")
    public BaseResponse<ChannelBrandHistoryAnalysisResponse> analysis(
            @PathVariable Long channelId,
            @ModelAttribute ChannelBrandHistorySearchCondition condition
    ) {
        return new BaseResponse<>(channelBrandHistoryService.analysis(channelId, condition));
    }
}
