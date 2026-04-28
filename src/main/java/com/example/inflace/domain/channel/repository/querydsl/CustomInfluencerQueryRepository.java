package com.example.inflace.domain.channel.repository.querydsl;

import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.response.InfluencerSearchResponse;
import org.springframework.data.domain.Slice;

public interface CustomInfluencerQueryRepository {

    Slice<InfluencerSearchResponse> getInfluencersWithSearchCondition(InfluencerSearchCondition searchCondition);
}
