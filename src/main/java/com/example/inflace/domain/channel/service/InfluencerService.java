package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.domain.channel.repository.querydsl.CustomInfluencerQueryRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InfluencerService {

    private final CustomInfluencerQueryRepository influencerQueryRepository;

    @ReadOnlyTransactional
    public Slice<GetInfluencerSearchResponse> getInfluencersWithSearchCondition(InfluencerSearchCondition searchCondition) {
        return influencerQueryRepository.getInfluencersWithSearchCondition(searchCondition);
    }
}
