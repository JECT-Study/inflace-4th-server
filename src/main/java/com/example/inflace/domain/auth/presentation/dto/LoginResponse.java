package com.example.inflace.domain.auth.presentation.dto;

import com.example.inflace.domain.channel.dto.UserChannelDetailsResponse;

public record LoginResponse(
        String accessToken,
        UserDetailsResponse userDetails,
        UserChannelDetailsResponse userChannelDetails
) {
}
