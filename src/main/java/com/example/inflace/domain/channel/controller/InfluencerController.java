package com.example.inflace.domain.channel.controller;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.InfluencerSearchResponse;
import com.example.inflace.domain.channel.service.InfluencerService;
import com.example.inflace.global.response.BaseResponse;
import com.example.inflace.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/influencers")
@RequiredArgsConstructor
public class InfluencerController implements InfluencerApi {

    private final InfluencerService influencerService;

    @Override
    @GetMapping
    public BaseResponse<SliceResponse<InfluencerSearchResponse>> getInfluencersWithSearchCondition(
            @ModelAttribute InfluencerSearchCondition searchCondition
    ) {
        return new BaseResponse<>(
                SliceResponse.from(
                        influencerService.getInfluencersWithSearchCondition(searchCondition),
                        searchCondition.sortCriteriaEnum().value(),
                        searchCondition.sortOrder().name()
                )
        );
    }
}
