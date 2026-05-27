package com.example.inflace.domain.user.presentation;

import com.example.inflace.domain.auth.presentation.dto.UserDetailsResponse;
import com.example.inflace.domain.channel.dto.response.UserChannelDetailsResponse;

public record GetUserMeResponse(
        UserDetailsResponse userDetails,
        UserChannelDetailsResponse userChannelDetails
) {
}
