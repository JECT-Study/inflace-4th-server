package com.example.inflace.domain.brandcollaboration.controller;

import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationSearchCondition;
import com.example.inflace.domain.brandcollaboration.dto.request.BrandCollaborationTrendsRequest;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationTrendsResponse;
import com.example.inflace.domain.brandcollaboration.dto.response.BrandCollaborationVideoResponse;
import com.example.inflace.domain.brandcollaboration.service.BrandCollaborationService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brand-collaborations")
@RequiredArgsConstructor
public class BrandCollaborationController implements BrandCollaborationApi {

    private final BrandCollaborationService brandCollaborationService;

    @Override
    @GetMapping
    public BaseResponse<CursorSliceResponse<BrandCollaborationVideoResponse>> search(
            @ModelAttribute BrandCollaborationSearchCondition condition
    ) {
        return new BaseResponse<>(brandCollaborationService.search(condition));
    }

    @Override
    @PostMapping("/trends")
    public BaseResponse<BrandCollaborationTrendsResponse> analyzeTrends(
            @RequestBody BrandCollaborationTrendsRequest request
    ) {
        return new BaseResponse<>(brandCollaborationService.analyzeTrends(request));
    }
}
