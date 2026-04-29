package com.example.inflace.domain.channel.repository.querydsl;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import org.springframework.data.domain.Slice;

public interface CustomInfluencerQueryRepository {

    Slice<GetInfluencerSearchResponse> getInfluencersWithSearchCondition(InfluencerSearchCondition searchCondition);
}
